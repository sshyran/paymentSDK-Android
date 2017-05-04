package de.wirecard.paymentsdkdemoapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import de.wirecard.paymentsdk.BuildConfig;
import de.wirecard.paymentsdk.CardBrand;
import de.wirecard.paymentsdk.WirecardCardFormFragment;
import de.wirecard.paymentsdk.WirecardClient;
import de.wirecard.paymentsdk.WirecardClientBuilder;
import de.wirecard.paymentsdk.WirecardEnvironment;
import de.wirecard.paymentsdk.WirecardException;
import de.wirecard.paymentsdk.WirecardInputFormsStateChangedListener;
import de.wirecard.paymentsdk.WirecardInputFormsStateManager;
import de.wirecard.paymentsdk.WirecardPaymentResponse;
import de.wirecard.paymentsdk.WirecardResponseError;
import de.wirecard.paymentsdk.WirecardResponseListener;
import de.wirecard.paymentsdk.WirecardTransactionType;
import de.wirecard.paymentsdk.models.CardToken;
import de.wirecard.paymentsdk.models.CustomerData;
import de.wirecard.paymentsdk.models.WirecardExtendedCardPayment;

public class MainActivity extends AppCompatActivity {

    private static final String ENCRYPTION_ALGORITHM = "HS256";
    private static final String UTF_8 = "UTF-8";

    private WirecardClient wirecardClient;
    private WirecardInputFormsStateManager wirecardInputFormsStateManager;
    private WirecardCardFormFragment wirecardCardFormFragment;
    private WirecardExtendedCardPayment wirecardExtendedCardPayment;

    private TextView stateLabel;
    private Switch securityCodeOnlySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateLabel = (TextView) findViewById(R.id.state);
        securityCodeOnlySwitch = (Switch) findViewById(R.id.securityCodeOnlySwitch);

        String environment = WirecardEnvironment.TEST.getValue();
        try {
            wirecardClient = WirecardClientBuilder.newInstance(this, environment)
                    .build();
        } catch (WirecardException exception) {
            Log.d(BuildConfig.APPLICATION_ID, "device is rooted");
        }

        initWirecardPaymentObject();

        if (savedInstanceState == null) {
            initWirecardCardFormFragment(false);
        }

        WirecardInputFormsStateChangedListener wirecardInputFormsStateChangedListener = new WirecardInputFormsStateChangedListener() {
            @Override
            public void onStateChanged(int code) {
                updateStateLabel(code);
                switch (code) {
                    case WirecardInputFormsStateChangedListener.CARD_NUMBER_FORM_FOCUS_GAINED:
                        //...
                        break;
                    case WirecardInputFormsStateChangedListener.CARD_VALID:
                        makeTransaction(false);
                        break;
                    case WirecardInputFormsStateChangedListener.SECURITY_CODE_VALID:
                        if (securityCodeOnlySwitch.isChecked())
                            makeTransaction(true);
                        //...
                }
            }
        };

        wirecardInputFormsStateManager =
                new WirecardInputFormsStateManager(MainActivity.this, wirecardInputFormsStateChangedListener);

        securityCodeOnlySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                initWirecardCardFormFragment(isChecked);
            }
        });
    }

    private void initWirecardCardFormFragment(boolean securityCodeOnly) {
        if (!securityCodeOnly) {
            wirecardExtendedCardPayment.setCardToken(null);
            wirecardCardFormFragment = new WirecardCardFormFragment.Builder(wirecardExtendedCardPayment)
                    .setLocale("de")
                    .build();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, wirecardCardFormFragment).commit();
        } else {
            wirecardExtendedCardPayment.setCardToken(new CardToken("4193258203791111", null));
            wirecardCardFormFragment = new WirecardCardFormFragment.Builder(wirecardExtendedCardPayment)
                    .setLocale("de")
                    .setExpirationDate("1219")
                    .setCardBrand(CardBrand.VISA)
                    .build();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, wirecardCardFormFragment).commit();
        }
    }

    private void initWirecardPaymentObject() {
        // for testing purposes only, do not store your merchant account ID and secret key inside app
        String timestamp = generateTimestamp();
        String merchantID = "33f6d473-3036-4ca5-acb5-8c64dac862d1";
        String secretKey = "9e0130f6-2e1e-4185-b0d5-dc69079c75cc";
        String requestID = UUID.randomUUID().toString();
        WirecardTransactionType transactionType = WirecardTransactionType.PURCHASE;
        BigDecimal amount = new BigDecimal("5.05");
        String currency = "EUR";

        String signature = generateSignatureV2(timestamp, merchantID, requestID,
                transactionType.getValue(), amount, currency, secretKey);

        wirecardExtendedCardPayment =
                new WirecardExtendedCardPayment(signature, timestamp, requestID,
                        merchantID, transactionType, amount, currency);
    }

    private void makeTransaction(boolean tokenAppended) {

        if (!tokenAppended) {
            //append last name
            CustomerData accountHolder = new CustomerData();
            accountHolder.setLastName("Doe");
            wirecardExtendedCardPayment.setAccountHolder(accountHolder);
        }
        //get WirecardExtendedCardPayment with appended card data from input fields
        wirecardExtendedCardPayment = wirecardCardFormFragment.getWirecardExtendedCardPayment();

        wirecardClient.makePayment(wirecardExtendedCardPayment, null, new WirecardResponseListener() {
            @Override
            public void onResponse(WirecardPaymentResponse wirecardPaymentResponse) {
                Log.d(BuildConfig.APPLICATION_ID, "success");
            }

            @Override
            public void onError(WirecardResponseError wirecardResponseError) {
                Log.d(BuildConfig.APPLICATION_ID, wirecardResponseError.getErrorMessage());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        wirecardInputFormsStateManager.stopReceivingEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        wirecardInputFormsStateManager.startReceivingEvents();
    }

    private static String generateSignatureV2(String timestamp, String merchantID, String requestID,
                                              String transactionType, BigDecimal amount, String currency,
                                              String secretKey) {

        String payload = ENCRYPTION_ALGORITHM.toUpperCase() + "\n" +
                "request_time_stamp=" + timestamp + "\n" +
                "merchant_account_id=" + merchantID + "\n" +
                "request_id=" + requestID + "\n" +
                "transaction_type=" + transactionType + "\n" +
                "requested_amount=" + amount + "\n" +
                "requested_amount_currency=" + currency.toUpperCase();

        try {
            byte[] encryptedPayload = encryptSignatureV2(payload, secretKey);
            return new String(Base64.encode(payload.getBytes(UTF_8), Base64.NO_WRAP), UTF_8)
                    + "." + new String(Base64.encode(encryptedPayload, Base64.NO_WRAP), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] encryptSignatureV2(String payload, String secretKey) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
            return mac.doFinal(payload.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[1];
    }

    private String generateTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        return new StringBuilder(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
                .format(calendar.getTime()))
                .insert(22, ":")
                .toString();
    }


    private void updateStateLabel(int code) {
        String state = "";
        switch (code) {
            case WirecardInputFormsStateChangedListener.CARD_NUMBER_FORM_FOCUS_GAINED:
                state = "CARD_NUMBER_FORM_FOCUS_GAINED";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_MONTH_FORM_FOCUS_GAINED:
                state = "EXPIRATION_MONTH_FORM_FOCUS_GAINED";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_YEAR_FORM_FOCUS_GAINED:
                state = "EXPIRATION_YEAR_FORM_FOCUS_GAINED";
                break;
            case WirecardInputFormsStateChangedListener.SECURITY_CODE_FORM_FOCUS_GAINED:
                state = "SECURITY_CODE_FORM_FOCUS_GAINED";
                break;
            case WirecardInputFormsStateChangedListener.CARD_NUMBER_FORM_FOCUS_LOST:
                state = "CARD_NUMBER_FORM_FOCUS_LOST";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_MONTH_FORM_FOCUS_LOST:
                state = "EXPIRATION_MONTH_FORM_FOCUS_LOST";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_YEAR_FORM_FOCUS_LOST:
                state = "EXPIRATION_YEAR_FORM_FOCUS_LOST";
                break;
            case WirecardInputFormsStateChangedListener.SECURITY_CODE_FORM_FOCUS_LOST:
                state = "SECURITY_CODE_FORM_FOCUS_LOST";
                break;
            case WirecardInputFormsStateChangedListener.CARD_BRAND_UNSUPPORTED:
                state = "CARD_TYPE_UNSUPPORTED";
                break;
            case WirecardInputFormsStateChangedListener.CARD_NUMBER_INVALID:
                state = "CARD_NUMBER_INVALID";
                break;
            case WirecardInputFormsStateChangedListener.CARD_NUMBER_INCOMPLETE:
                state = "CARD_NUMBER_INCOMPLETE";
                break;
            case WirecardInputFormsStateChangedListener.CARD_NUMBER_VALID:
                state = "CARD_NUMBER_VALID";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_MONTH_INCOMPLETE:
                state = "EXPIRATION_MONTH_INCOMPLETE";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_MONTH_VALID:
                state = "EXPIRATION_MONTH_VALID";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_YEAR_INCOMPLETE:
                state = "EXPIRATION_YEAR_INCOMPLETE";
                break;
            case WirecardInputFormsStateChangedListener.EXPIRATION_YEAR_VALID:
                state = "EXPIRATION_YEAR_VALID";
                break;
            case WirecardInputFormsStateChangedListener.SECURITY_CODE_INCOMPLETE:
                state = "SECURITY_CODE_INCOMPLETE";
                break;
            case WirecardInputFormsStateChangedListener.SECURITY_CODE_VALID:
                state = "SECURITY_CODE_VALID";
                break;
            case WirecardInputFormsStateChangedListener.CARD_VALID:
                state = "CARD_VALID";
                break;

        }
        stateLabel.setText(state);
    }
}
