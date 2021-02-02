package android.example.bluethroughv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Alias extends AppCompatActivity {

    EditText mAlias;
    Button mBotonAlias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alias2);
        mAlias = findViewById(R.id.et_Alias);
        mBotonAlias = findViewById(R.id.bt_EnviarAlias);
        mBotonAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String alias = mAlias.getText().toString();
                Intent intent = new Intent();
                intent.putExtra("ALIAS", alias);
                setResult(2,intent);
                finish();
            }
        });

    }
}