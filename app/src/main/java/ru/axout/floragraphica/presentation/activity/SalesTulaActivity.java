package ru.axout.floragraphica.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import ru.axout.floragraphica.CaptureAct;
import ru.axout.floragraphica.R;
import ru.axout.floragraphica.data.RoomDB;
import ru.axout.floragraphica.data.SalesData;
import ru.axout.floragraphica.data.TulaData;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SalesTulaActivity extends AppCompatActivity {

    RoomDB database;
    Toast toast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_food);

        // Initialize BD
        database = RoomDB.getInstance(this);
        Button btScan = findViewById(R.id.bt_scan_to_food);

        btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanCode();
            }
        });
    }

    private void scanCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Сканирование...");
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            final String scanResult = result.getContents();
            if (scanResult != null) {
                try {
                    addScanDataToDB(scanResult);
                } catch (Exception e) {
                    showMessageCenter("ОТКАЗ");
                }
                scanCode();
            }
            else {
                showMessage("Нет результата");
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void addScanDataToDB(String scanResult) {
        int cod = Integer.parseInt(scanResult);
        int sortID = cod / 100000;
        int packNumber = cod % 100000;
        TulaData tulaData;
        String sort;

        // Получение строки из table_tula по sortID
        tulaData = database.tulaDao().getWhereSortIDAndPackNum(sortID, packNumber);
        // Получение сорта из полученной строки
        sort = tulaData.getSort();
        // Добавление в table_sales
        SalesData salesData = new SalesData();
        salesData.setSortID(sortID);
        salesData.setSort(sort);
        salesData.setPackageNumber(packNumber);
        salesData.setDateAdded(getFormatDate());
        // Вставка данных (картежа) в БД (Insert text in database)
        database.salesDao().insert(salesData);

        // Удаление из table_tula отправленной на Варшавку упаковки
        database.tulaDao().delete(tulaData);

        showMessage("Продано");
    }

    // Форматирование даты под следующий вид: "dd.MM.yyyy"
    // Фиксация даты добавления упаковки
    private String getFormatDate() {
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy");
        return formatForDateNow.format(dateNow);
    }

    private void showMessage(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(SalesTulaActivity.this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showMessageCenter(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(SalesTulaActivity.this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
