package com.movemate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Get the File object pointing to the user's local profile picture file.
     */
    public static File getProfileImageFile(Context context) {
        String email = SessionManager.getUserEmail(context);
        if (email == null || email.isEmpty()) {
            com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                email = firebaseUser.getEmail();
            }
        }
        // If still null, use a generic fallback (though email should be present)
        if (email == null || email.isEmpty()) {
            email = "default_user";
        }
        
        File dir = new File(context.getFilesDir(), "profile_pictures");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Create safe filename
        String safeEmail = email.replace("@", "_at_").replace(".", "_dot_");
        return new File(dir, "profile_" + safeEmail + ".jpg");
    }

    /**
     * Loads the profile image into the given ImageView.
     */
    public static void loadProfileImage(Context context, ImageView imageView) {
        File imgFile = getProfileImageFile(context);
        if (imgFile != null && imgFile.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (myBitmap != null) {
                    imageView.setImageBitmap(myBitmap);
                    imageView.setPadding(0, 0, 0, 0); // Remove padding if custom image is set
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    /**
     * Saves an image from a content URI to internal storage.
     */
    public static boolean saveImageFromUri(Context context, Uri uri) {
        File targetFile = getProfileImageFile(context);
        if (targetFile == null) return false;

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            if (inputStream == null) return false;
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            Log.e("ImageUtils", "Failed to save image", e);
            return false;
        }
    }
}
