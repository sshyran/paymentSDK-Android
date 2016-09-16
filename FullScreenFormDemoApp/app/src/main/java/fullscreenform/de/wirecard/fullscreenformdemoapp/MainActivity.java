package fullscreenform.de.wirecard.fullscreenformdemoapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import de.wirecard.paymentsdk.WirecardClient;
import de.wirecard.paymentsdk.WirecardClientBuilder;
import de.wirecard.paymentsdk.WirecardEnvironment;
import de.wirecard.paymentsdk.WirecardException;
import de.wirecard.paymentsdk.WirecardPaymentResponse;
import de.wirecard.paymentsdk.WirecardPaymentType;
import de.wirecard.paymentsdk.WirecardResponseError;
import de.wirecard.paymentsdk.WirecardResponseListener;
import de.wirecard.paymentsdk.WirecardTransactionType;
import de.wirecard.paymentsdk.models.WirecardCardPayment;
import de.wirecard.paymentsdk.models.WirecardPayPalPayment;
import de.wirecard.paymentsdk.models.WirecardPayment;
import de.wirecard.paymentsdk.models.WirecardSepaPayment;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WirecardClient wirecardClient;
    private TextView resultLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String environment = WirecardEnvironment.TEST.getValue();
        try {
            wirecardClient = WirecardClientBuilder.newInstance(this, environment)
                    .build();
        } catch (WirecardException exception) {
            Log.d(de.wirecard.paymentsdk.BuildConfig.APPLICATION_ID, "device is rooted");
        }

        resultLabel = (TextView) findViewById(R.id.result);
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        findViewById(R.id.card).setOnClickListener(this);
        findViewById(R.id.paypal).setOnClickListener(this);
        findViewById(R.id.sepa).setOnClickListener(this);
    }


    private void makeTransaction(WirecardPaymentType wirecardPaymentType, WirecardTransactionType transactionType) {

        String merchantID;
        String secretKey;
        String data;
        String signature;

        // for testing purposes only, do not store your merchant account ID and secret key inside app
        String timestamp = generateTimestamp();
        String requestID = UUID.randomUUID().toString();
        BigDecimal amount = new BigDecimal(10);
        String currency = "EUR";


        WirecardPayment wirecardPayment = null;

        switch (wirecardPaymentType) {
            case CARD:

                merchantID = "33f6d473-3036-4ca5-acb5-8c64dac862d1";
                secretKey = "9e0130f6-2e1e-4185-b0d5-dc69079c75cc";

                data = timestamp + requestID + merchantID +
                        transactionType.getValue() + amount + currency + secretKey;
                signature = generateSignature(data);

                wirecardPayment = new WirecardCardPayment(timestamp, requestID, merchantID,
                        transactionType, amount,
                        currency, signature);
                ((WirecardCardPayment) wirecardPayment).setAttempt3d(true);
                break;
            case PAYPAL:

                merchantID = "9abf05c1-c266-46ae-8eac-7f87ca97af28";
                secretKey = "5fca2a83-89ca-4f9e-8cf7-4ca74a02773f";

                data = timestamp + requestID + merchantID +
                        transactionType.getValue() + amount + currency + secretKey;
                signature = generateSignature(data);

                wirecardPayment = new WirecardPayPalPayment(timestamp, requestID, merchantID,
                        transactionType, amount,
                        currency, signature);
                break;
            case SEPA:

                merchantID = "4c901196-eff7-411e-82a3-5ef6b6860d64";
                secretKey = "ecdf5990-0372-47cd-a55d-037dccfe9d25";

                data = timestamp + requestID + merchantID +
                        transactionType.getValue() + amount + currency + secretKey;
                signature = generateSignature(data);

                wirecardPayment = new WirecardSepaPayment(timestamp, requestID, merchantID, transactionType,
                        amount, currency, signature, "creditorID", "mandateID", new Date(),
                        "merchantName", null);
                break;
        }

        wirecardClient.makePayment(wirecardPayment, null, new WirecardResponseListener() {
            @Override
            public void onResponse(WirecardPaymentResponse wirecardPaymentResponse) {
                Log.d(de.wirecard.paymentsdk.BuildConfig.APPLICATION_ID, "response received");

                resultLabel.setText(wirecardPaymentResponse.getTransactionState().getValue() + "\n"
                        + wirecardPaymentResponse.getStatuses().getStatus()[0].getDescription());
            }

            @Override
            public void onError(WirecardResponseError wirecardResponseError) {
                Log.d(de.wirecard.paymentsdk.BuildConfig.APPLICATION_ID, wirecardResponseError.getErrorMessage());

                resultLabel.setText(wirecardResponseError.getErrorMessage());
            }
        });
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.card:
                makeTransaction(WirecardPaymentType.CARD, WirecardTransactionType.PURCHASE);
                break;
            case R.id.paypal:
                makeTransaction(WirecardPaymentType.PAYPAL, WirecardTransactionType.DEBIT);
                break;
            case R.id.sepa:
                makeTransaction(WirecardPaymentType.SEPA, WirecardTransactionType.PENDING_DEBIT);
                break;
        }
    }
}
