package com.ichi2.anki.tests;

import com.ichi2.anki.StudyOptions;
import com.ichi2.anki.Reviewer;

import com.jayway.android.robotium.solo.Solo;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.lang.System;

public class PerformanceTest extends ActivityInstrumentationTestCase2<StudyOptions> {

	private static final String TAG = "AnkiDroidTest";

	private Solo solo;

	public PerformanceTest() {
		super("com.ichi2.anki", StudyOptions.class);
	}

	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
	}

	public void test1() throws Exception {
		assertTrue(solo.waitForActivity("StudyOptions", 15000));
		ArrayList<TextView> myv = solo.getCurrentTextViews(null);
		assertNotNull(myv);
		for (int i = 0; i < myv.size(); i++) {
			assertNotNull(myv.get(i));
			Log.i(TAG, "Text: " + myv.get(i).getText().toString());
		}
//		assertTrue(solo.waitForText("slow-vacuum"));
		//solo.sendKey(Solo.MENU);
		//solo.clickOnText("Open Deck");
		//assertTrue(solo.searchText("slow_memory"));
	}

	@Override
	public void tearDown() throws Exception {
		try {
			solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		getActivity().finish();
		super.tearDown();
	}
}
