package com.heshicaihao.zxing.decode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.heshicaihao.zxing.R;


/**
 * 二维码解析管理。
 */
public class DecodeManager {

    public void showPermissionDeniedDialog(final Context context) {
        // 权限在安装时被关闭了，如小米手机
        new AlertDialog.Builder(context).setTitle(R.string.hsc_zxing_notification)
                .setMessage(R.string.hsc_zxing_camera_not_open)
                .setPositiveButton(R.string.hsc_zxing_positive_button_know, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (context instanceof Activity) {
                            ((Activity) context).finish();
                        }
                    }
                })
                .show();
    }

    public void showResultDialog(Activity activity, String resultString, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(activity).setTitle(R.string.hsc_zxing_notification)
                .setMessage(resultString)
                .setPositiveButton(R.string.hsc_zxing_positive_button_confirm, listener)
                .show();
    }

    public void showCouldNotReadQrCodeFromScanner(Context context, final OnRefreshCameraListener listener) {
        new AlertDialog.Builder(context).setTitle(R.string.hsc_zxing_notification)
                .setMessage(R.string.hsc_zxing_could_not_read_qr_code_from_scanner)
                .setPositiveButton(R.string.hsc_zxing_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.refresh();
                        }
                    }
                })
                .show();
    }

    public void showCouldNotReadQrCodeFromPicture(Context context) {
        new AlertDialog.Builder(context).setTitle(R.string.hsc_zxing_notification)
                .setMessage(R.string.hsc_zxing_could_not_read_qr_code_from_picture)
                .setPositiveButton(R.string.hsc_zxing_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public interface OnRefreshCameraListener {
        void refresh();
    }
}
