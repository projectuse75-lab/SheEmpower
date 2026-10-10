package com.sheempower.app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;

import com.bumptech.glide.Glide;
import com.sheempower.app.R;

public class ProfileDetailsDialog extends Dialog {

    private ImageView ivImage;
    private AppCompatTextView tvName;
    private AppCompatTextView tvFatherName;
    private AppCompatTextView tvClass;
    private Button btnEdit;
    private Button btnShare;
    private Button btnClose;
    private Button btnCloseBottom;

    // Data
    private Uri imageUri;
    private String userName;
    private String fatherName;
    private String userClass;

    // Callbacks
    private OnShareClickListener shareListener;
    private OnEditClickListener editListener;

    public interface OnShareClickListener {
        void onShareClick();
    }

    public interface OnEditClickListener {
        void onEditClick();
    }

    // Constructor
    public ProfileDetailsDialog(Context context,
                                String name,
                                String fatherName,
                                String userClass,
                                Uri imageUri,
                                OnShareClickListener shareListener,
                                OnEditClickListener editListener) {
        super(context);

        this.userName = name;
        this.fatherName = fatherName;
        this.userClass = userClass;
        this.imageUri = imageUri;
        this.shareListener = shareListener;
        this.editListener = editListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_profile_details);

        if (getWindow() != null) {
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // Initialize views
        ivImage = findViewById(R.id.iv_dialog_image);
        tvName = findViewById(R.id.tv_dialog_name);
        tvFatherName = findViewById(R.id.tv_dialog_father_name);
        tvClass = findViewById(R.id.tv_dialog_class);
        btnEdit = findViewById(R.id.btn_dialog_edit);
        btnShare = findViewById(R.id.btn_dialog_share);
        btnClose = findViewById(R.id.btn_dialog_close);
        btnCloseBottom = findViewById(R.id.btn_dialog_close_bottom);

        // Populate data
        if (ivImage != null && imageUri != null) {
            Glide.with(getContext()).load(imageUri).into(ivImage);
        }

        if (tvName != null) {
            String display = (userName != null && !userName.trim().isEmpty()) ? userName : "---";
            tvName.setText("👤 نام / Name: " + display);
        }

        if (tvFatherName != null) {
            String display = (fatherName != null && !fatherName.trim().isEmpty()) ? fatherName : "---";
            tvFatherName.setText("👨‍👦 والد کا نام / Father Name: " + display);
        }

        if (tvClass != null) {
            String display = (userClass != null && !userClass.trim().isEmpty()) ? userClass : "---";
            tvClass.setText("🎓 کلاس / Class: " + display);
        }

        // Click listeners
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dismiss();
                if (editListener != null) {
                    editListener.onEditClick();
                }
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                if (shareListener != null) {
                    shareListener.onShareClick();
                }
                dismiss();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        if (btnCloseBottom != null) {
            btnCloseBottom.setOnClickListener(v -> dismiss());
        }
    }
}
