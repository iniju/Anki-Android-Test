package com.ichi2.libanki.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Storage;

import android.content.Context;
import android.util.Log;

public class Shared {
	public static final String TAG = "AnkiDroidTest";
	
	public static File getUpgradeDeckPath(Context ctx) throws IOException {
		return getUpgradeDeckPath(ctx, "anki12.anki");
	}
	public static File getUpgradeDeckPath(Context ctx, String name) throws IOException {
		File srcdir = ctx.getExternalFilesDir(null);
		Log.i("AnkiDroidTest", "Target dir: " + srcdir.getAbsolutePath());
		File dst = File.createTempFile("tmp", ".anki2", srcdir);
		InputStream is = ctx.getResources().getAssets().open(name);
		byte[] buf = new byte[32768];
		OutputStream output = new BufferedOutputStream(new FileOutputStream(dst));
		int len;
		while ((len = is.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
		output.close();
		is.close();
		return dst;
	}
	
	public static Collection getEmptyDeck(Context ctx) {
		File dstdir = ctx.getExternalFilesDir(null);
		File dst;
		try {
			dst = File.createTempFile("empty", ".anki2", dstdir);
		} catch (IOException e) {
			Log.e(TAG, "Shared.getEmptyDeck: ", e);
			return null;
		}
		if (dst.exists()) {
			dst.delete();
		}
		return Storage.Collection(dst.getAbsolutePath());
	}

    public static int[] toPrimitiveInt(Integer[] array) {
        int[] results = new int[array.length];
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                results[i] = array[i].intValue();
            }
        }
        return results;
    }
}
