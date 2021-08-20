package com.demo.coursemoney;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.coursemoney.api.ApiFactory;
import com.demo.coursemoney.pojo.Eur;
import com.demo.coursemoney.pojo.JsonResponse;
import com.demo.coursemoney.pojo.Usd;
import com.demo.coursemoney.pojo.Valute;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ApiFactory apiFactory;

    private double valueUSD;// здесь храним загруженное значение рубль/доллар
    private double valueEUR;// здесь храним загруженное значение рубль/евро

    private Spinner spinnerInputValute;
    private Spinner spinnerOutputValute;
    private EditText editTextInput;
    private TextView textViewOutput;
    private ProgressBar progressBar;

    private ArrayList<String> valutesSpinnerInput;// массив для всех валют
    private ArrayList<String> valutesSpinnerOutput;// массив для валют в спиннере spinnerOutputValute

    private double numberOutput;// значение в textViewOutput - на выходе

    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);// Отключим тёмную тему

        spinnerInputValute = findViewById(R.id.spinnerInputValute);
        spinnerOutputValute = findViewById(R.id.spinnerOutputValute);
        editTextInput = findViewById(R.id.editTextInput);
        textViewOutput = findViewById(R.id.textViewOutput);
        progressBar = findViewById(R.id.progressBar);

        compositeDisposable = new CompositeDisposable();
        valutesSpinnerOutput = new ArrayList<>();
        numberOutput=0;

        valutesSpinnerInput = new ArrayList<>();// добавим в массив все наши валюты
        valutesSpinnerInput.add(getString(R.string.RUB));
        valutesSpinnerInput.add(getString(R.string.USD));
        valutesSpinnerInput.add(getString(R.string.EUR));
        ArrayAdapter<String> arrayAdapterIn = new ArrayAdapter(getApplicationContext(), R.layout.spinner, valutesSpinnerInput);// адаптер для установки массива в спиннер spinnerInputValute
        spinnerInputValute.setAdapter(arrayAdapterIn);// установим адаптер для спиннера

        setValutesSpinnerOutput();// установим нужные значения в выходном спиннере с валютами

        // установим слушатель для spinnerOutputValute
        spinnerOutputValute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (editTextInput.getText().toString().isEmpty()){
                    textViewOutput.setText("");
                }
                else{
                    convertValute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    // метод для установки значений в спиннер spinnerOutputValute в зависимости от выбранного значения в spinnerInputValute
    public void setValutesSpinnerOutput(){
        // создаём слушатель событий для spinnerInputValute
        spinnerInputValute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                valutesSpinnerOutput.clear();
                if (position == 0){
                    valutesSpinnerOutput.add(getString(R.string.USD));
                    valutesSpinnerOutput.add(getString(R.string.EUR));
                } else if (position == 1){
                    valutesSpinnerOutput.add(getString(R.string.RUB));
                    valutesSpinnerOutput.add(getString(R.string.EUR));
                } else if (position == 2){
                    valutesSpinnerOutput.add(getString(R.string.RUB));
                    valutesSpinnerOutput.add(getString(R.string.USD));
                }
                // создаём адаптер для массива с валютами, который будем устанавливать в спиннере spinnerOutputValute
                ArrayAdapter<String> arrayAdapterOut = new ArrayAdapter(getApplicationContext(), R.layout.spinner, valutesSpinnerOutput);
                spinnerOutputValute.setAdapter(arrayAdapterOut);

                if (editTextInput.getText().toString().isEmpty()){
                    textViewOutput.setText("");
                }
                else{
                    convertValute();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // метод при нажатии на кнопку
    public void onClickConvert(View view) {

            // если поле не заполнено, то выскочит уведомление и установится пустое поле
            if (editTextInput.getText().toString().isEmpty()){
                textViewOutput.setText("");
                Toast.makeText(getApplicationContext(), getString(R.string.toast), Toast.LENGTH_SHORT).show();
            }
            // если пользователь заполнил поле, то будем конвертировать значение в зависимости от выбранной валюты
            else{
                convertValute();
            }

    }


    public void convertValute(){

        // получим retrofit
        apiFactory = ApiFactory.getInstanceRetrofit();
        // получим Observable<JsonResponse> с помощью метода getJsonResponse()
        Observable<JsonResponse> jsonResponse= apiFactory.getApiService().getJsonResponse();

        Disposable disposable = jsonResponse
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                })
                .doAfterTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                })
                .subscribe(new Consumer<JsonResponse>() {
                    @Override
                    public void accept(JsonResponse jsonResponse) throws Exception {

                        Valute valute = jsonResponse.getValute();// получим Валюты из JSON-ответа
                        Usd usd = valute.getUsd();// получим доллар
                        valueUSD = usd.getValue();// получим значение доллара
                        Eur eur = valute.getEur();// получим евро
                        valueEUR = eur.getValue();// получим значение евро

                        Log.i("My", "USD "+ valueUSD);
                        Log.i("My", "EUR "+ valueEUR);

                        double numberInput = 0;// numberInput - число, которое вводит пользователь
                        numberInput = Double.parseDouble(editTextInput.getText().toString().trim());// получим введённое пользователем число

                        String valuteInput = spinnerInputValute.getSelectedItem().toString();// получим выбранную пользователем валюту на входе
                        String valuteOutput = spinnerOutputValute.getSelectedItem().toString();// получим выбранную пользователем валюту на выходе

                        // если валюты на входе и выходе совпадают
                        if (valuteInput.equals(valuteOutput)){
                            numberOutput = numberInput;
                        }
                        // рубли на входе
                        if (valuteInput.equals(getString(R.string.RUB)) && valuteOutput.equals(getString(R.string.USD))){
                            numberOutput = numberInput/valueUSD;
                        }
                        else if (valuteInput.equals(getString(R.string.RUB)) && valuteOutput.equals(getString(R.string.EUR))){
                            numberOutput = numberInput/valueEUR;
                        }
                        // доллары на входе
                        else if (valuteInput.equals(getString(R.string.USD)) && valuteOutput.equals(getString(R.string.RUB))){
                            numberOutput = numberInput*valueUSD;
                        }
                        else if (valuteInput.equals(getString(R.string.USD)) && valuteOutput.equals(getString(R.string.EUR))){
                            numberOutput = numberInput*valueUSD/valueEUR;
                        }
                        // евро на входе
                        else if (valuteInput.equals(getString(R.string.EUR)) && valuteOutput.equals(getString(R.string.RUB))){
                            numberOutput = numberInput*valueEUR;
                        }
                        else if(valuteInput.equals(getString(R.string.EUR)) && valuteOutput.equals(getString(R.string.USD))){
                            numberOutput = numberInput*valueEUR/valueUSD;
                        }
                        textViewOutput.setText(String.format(Locale.getDefault(),"%.2f",numberOutput));// установим в поле textViewOutput значение numberOutput

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(getApplicationContext(), getString(R.string.internet_error), Toast.LENGTH_SHORT).show();
                        Log.i("My", throwable.getMessage());
                    }
                });

        compositeDisposable.add(disposable);

    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        Log.i("My", "dispose");
        super.onDestroy();
    }
}