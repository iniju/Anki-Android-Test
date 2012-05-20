package com.ichi2.libanki.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.ichi2.libanki.Storage;

public class UpgradeTestCase extends InstrumentationTestCase {
	@MediumTest
	public void test_check() {
		File dst = null;
		try {
			dst = Shared.getUpgradeDeckPath(getInstrumentation().getContext());
			assertTrue(Storage.check(dst.getAbsolutePath()));
			// if it's correct, will fail
			FileOutputStream fos = new FileOutputStream(dst);
			fos.write("foo".getBytes());
			fos.close();
			assertFalse(Storage.check(dst.getAbsolutePath()));
		} catch (IOException e) {
			Log.e(Shared.TAG, "UpgradeTestCase.test_check: ", e);
			throw new RuntimeException(e);
		} finally {
			if (dst != null) {
				dst.delete();
			}
		}
	}
}
