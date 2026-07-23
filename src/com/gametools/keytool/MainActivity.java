package com.gametools.keytool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private EditText edtPassword;
    private EditText edtAlias;
    private Button btnGenerate;
    private Button btnChoosePath;
    private TextView txtStatus;
    private TextView txtSelectedPath;

    // Переменная для хранения выбранного пути (по умолчанию корень памяти)
    private String customPath = ""; 
    private static final int REQUEST_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        edtPassword = (EditText) findViewById(R.id.edtPassword);
        edtAlias = (EditText) findViewById(R.id.edtAlias);
        btnGenerate = (Button) findViewById(R.id.btnGenerate);
        btnChoosePath = (Button) findViewById(R.id.btnChoosePath);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtSelectedPath = (TextView) findViewById(R.id.txtSelectedPath);

        // Инициализируем путь по умолчанию
        customPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        txtSelectedPath.setText(customPath);

        // Нажатие на кнопку выбора папки
        btnChoosePath.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showFolderPickerDialog(new File(customPath));
				}
			});

        // Нажатие на кнопку генерации
        btnGenerate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkPermissionsAndGenerate();
				}
			});
    }

    /**
     * Создает и показывает диалоговое окно для выбора папки
     */
    private void showFolderPickerDialog(final File currentDir) {
        if (!currentDir.exists() || !currentDir.isDirectory()) {
            Toast.makeText(this, "Папка недоступна", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<String> dirNames = new ArrayList<String>();
        final List<File> dirFiles = new ArrayList<File>();

        // Добавляем пункт возврата назад, если мы не в самом корне системы
        if (currentDir.getParentFile() != null) {
            dirNames.add(".. (Вверх)");
            dirFiles.add(currentDir.getParentFile());
        }

        // Читаем список подпапок
        File[] files = currentDir.listFiles();
        if (files != null) {
            List<File> tempDirs = new ArrayList<File>();
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    tempDirs.add(file);
                }
            }
            // Сортируем папки по алфавиту
            Collections.sort(tempDirs);
            for (File dir : tempDirs) {
                dirNames.add(dir.getName() + "/");
                dirFiles.add(dir);
            }
        }

        String[] options = dirNames.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Текущая папка:\n" + currentDir.getAbsolutePath());

        builder.setItems(options, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Пользователь выбрал папку из списка — переходим внутрь неё
					showFolderPickerDialog(dirFiles.get(which));
				}
			});

        // Кнопка подтверждения выбора текущей папки
        builder.setPositiveButton("Выбрать эту папку", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					customPath = currentDir.getAbsolutePath();
					txtSelectedPath.setText(customPath);
				}
			});

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void checkPermissionsAndGenerate() {
        if (Build.VERSION.SDK_INT >= 23) {
            String permission = "android.permission.WRITE_EXTERNAL_STORAGE";
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, REQUEST_PERMISSION_CODE);
                return;
            }
        }
        startKeyGeneration();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startKeyGeneration();
            } else {
                txtStatus.setTextColor(0xFFFF0000);
                txtStatus.setText("Нет разрешения на запись в память!");
                Toast.makeText(this, "Нет разрешения на запись в память!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startKeyGeneration() {
        String password = edtPassword.getText().toString().trim();
        String alias = edtAlias.getText().toString().trim();

        if (password.length() == 0 || alias.length() == 0) {
            txtStatus.setTextColor(0xFFFF0000);
            txtStatus.setText("Пожалуйста, заполните все поля!");
            return;
        }

        if (password.length() < 6) {
            txtStatus.setTextColor(0xFFFF0000);
            txtStatus.setText("Пароль должен быть не менее 6 символов!");
            return;
        }

        txtStatus.setText("");

        try {
            // Теперь используем переменную customPath, выбранную пользователем
            KeyGeneratorUtil.createKeysAndCert(customPath, password, alias);

            String successMsg = "Файлы успешно созданы!\n\nПуть к файлам:\n" + customPath;
            txtStatus.setTextColor(0xFF00AA00); 
            txtStatus.setText(successMsg);

            Toast.makeText(this, "Успешно создано!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            txtStatus.setTextColor(0xFFFF0000); 
            txtStatus.setText("Ошибка генерации: " + e.toString());
        }
    }
}

