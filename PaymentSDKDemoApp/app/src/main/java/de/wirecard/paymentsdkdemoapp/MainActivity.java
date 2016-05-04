package de.wirecard.paymentsdkdemoapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import de.wirecard.paymentsdk.BuildConfig;
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
import de.wirecard.paymentsdk.models.CustomerData;
import de.wirecard.paymentsdk.models.WirecardExtendedCardPayment;

public class MainActivity extends AppCompatActivity {

    private WirecardClient wirecardClient;
    private WirecardInputFormsStateManager wirecardInputFormsStateManager;
    private WirecardCardFormFragment wirecardCardFormFragment;

    private TextView stateLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateLabel = (TextView) findViewById(R.id.state);

        String environment = WirecardEnvironment.TEST.getValue();
        try {
            wirecardClient = WirecardClientBuilder.newInstance(this, environment)
                    .build();
        } catch (WirecardException exception) {
            Log.d(BuildConfig.APPLICATION_ID, "device is rooted");
        }

        if (savedInstanceState == null) {
            wirecardCardFormFragment = new WirecardCardFormFragment();
            wirecardCardFormFragment.setLocale("de");
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, wirecardCardFormFragment).commit();
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
                        makeTransaction();
                        break;
                    //...
                }
            }
        };

        wirecardInputFormsStateManager =
                new WirecardInputFormsStateManager(MainActivity.this, wirecardInputFormsStateChangedListener);

    }

    private void makeTransaction() {

        // for testing purposes only, do not store your merchant account ID and secret key inside app
        String timestamp = generateTimestamp();
        String merchantID = "";
        String secretKey = "";
        String requestID = UUID.randomUUID().toString();
        WirecardTransactionType transactionType = WirecardTransactionType.AUTHORIZATION_ONLY;
        BigDecimal amount = new BigDecimal(0);
        String currency = "EUR";

        String data = timestamp + requestID + merchantID +
                transactionType.getValue() + amount + currency + secretKey;

        String signature = generateSignature(data);

        WirecardExtendedCardPayment wirecardExtendedCardPayment =
                new WirecardExtendedCardPayment(timestamp, requestID, merchantID,
                        transactionType, amount,
                        currency, signature);

        //append last name
        CustomerData accountHolder = new CustomerData();
        accountHolder.setLastName("Doe");
        wirecardExtendedCardPayment.setAccountHolder(accountHolder);

        //append card data from input fields
        wirecardExtendedCardPayment = wirecardCardFormFragment.appendCardData(wirecardExtendedCardPayment);

        wirecardClient.makePayment(wirecardExtendedCardPayment, null, new WirecardResponseListener() {
            @Override
            public void onSuccess(WirecardPaymentResponse wirecardPaymentResponse) {
                Log.d(BuildConfig.APPLICATION_ID, "success");
            }

            @Override
            public void onError(WirecardResponseError wirecardResponseError) {
                Log.d(BuildConfig.APPLICATION_ID, "error");
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

    private String generateTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(timeZone);
        return simpleDateFormat.format(calendar.getTime());
    }

    public String generateSignature(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        try {
            md.update(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] mbs = md.digest();
        sb.setLength(0);
        for (int i = 0; i < mbs.length; i++) {
            sb.append(Integer.toString((mbs[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
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
            case WirecardInputFormsStateChangedListener.CARD_TYPE_UNSUPPORTED:
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
