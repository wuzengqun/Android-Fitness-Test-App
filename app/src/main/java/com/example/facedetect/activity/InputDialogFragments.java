package com.example.facedetect.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.example.facedetect.R;

public class InputDialogFragments extends DialogFragment {
    private EditText inputEditText1, inputEditText2, inputEditText3;
    private OnInputListener onInputListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_input, null);

        inputEditText1 = view.findViewById(R.id.inputEditText1);
        inputEditText2 = view.findViewById(R.id.inputEditText2);
        inputEditText3 = view.findViewById(R.id.inputEditText3);
        Button confirmButton = view.findViewById(R.id.confirmButton);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText1 = inputEditText1.getText().toString();
                String inputText2 = inputEditText2.getText().toString();
                String inputText3 = inputEditText3.getText().toString();
                if (onInputListener != null) {
                    onInputListener.onInput(inputText1, inputText2, inputText3);
                }
                dismiss();
            }
        });

        builder.setView(view)
                .setTitle("请输入个人信息");
                //.setMessage("Please enter your information:");

        return builder.create();
    }

    public interface OnInputListener {
        void onInput(String input, String input_class, String input_card);
    }

    public void setOnInputListener(OnInputListener listener) {
        onInputListener = listener;
    }
}
