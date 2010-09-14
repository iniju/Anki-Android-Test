package com.ichi2.anki.tests;

import com.ichi2.anki.StudyOptions;
import com.ichi2.anki.Reviewer;

import com.jayway.android.robotium.solo.Solo;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.util.Log;
import android.test.suitebuilder.annotation.LargeTest;

import java.util.ArrayList;
import java.lang.System;

public class PerformanceTest extends ActivityInstrumentationTestCase2<StudyOptions> {

	private static final String TAG = "AnkiDroidTest";
	private static final String DECK = "slow-vacuum";
	private static final int ANSCARDS = 200;

	private Solo solo;

	private void helperPickDeck(String deck) {
		assertTrue(solo.waitForActivity("StudyOptions", 15000));
		if (solo.searchText("Load Other Deck", 1)) {
			solo.clickOnButton("Load Other Deck");
		} else {
			assertTrue(solo.waitForText("Study Options"));
			solo.sendKey(solo.MENU);
			assertTrue(solo.waitForText("Open Deck"));
			solo.clickOnText("Open Deck");
		}
		assertTrue(solo.waitForActivity("DeckPicker", 60000));
		ArrayList<ListView> deck_picker_list = solo.getCurrentListViews();
		assertNotNull(deck_picker_list);
		ListView lv = deck_picker_list.get(0);
		View pg;
		View tv;
		View v;
		boolean allDone = false;
		boolean notFound = true;
		int decki = 0;
		while (!allDone) {
			allDone = true;
			for (int i = 0; i < lv.getChildCount(); i++) {
				v = lv.getChildAt(i);
				assertTrue(v instanceof LinearLayout);
				tv = ((LinearLayout)v).getChildAt(0);
				assertTrue(tv instanceof TextView);
				v = ((LinearLayout)v).getChildAt(1);
				assertTrue(v instanceof LinearLayout);
				pg = ((LinearLayout)v).getChildAt(2);
				assertTrue(pg instanceof ProgressBar);
				if (notFound && ((TextView)tv).getText().toString().equals(deck)) {
					notFound = false;
					decki = i;
					Log.i(TAG, "Found requested deck in position " + decki);
				}
				if (((ProgressBar)pg).getVisibility() == View.VISIBLE) {
					allDone = false;
				}
			}
		}
		//assertTrue(solo.waitForText(deck));
		//solo.clickOnText(deck);

		solo.clickInList(decki);
		assertTrue(solo.waitForActivity("StudyOptions", 10000));

	}

	public PerformanceTest() {
		super("com.ichi2.anki", StudyOptions.class);
	}

	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@LargeTest
	public void test1() throws Exception {
		helperPickDeck(DECK);
		Log.i(TAG, "Start reviewing deck " + DECK + " - " + ANSCARDS + " iterations.");
		solo.clickOnButton("Start Reviewing");
		assertTrue(solo.waitForActivity("Reviewer", 5));
		Reviewer rev = (Reviewer)(solo.getCurrentActivity());
		long tottime = 0;

		for (int j = 0; j < ANSCARDS; j++) {
			assertTrue(solo.waitForText("Show Answer"));
			solo.clickOnButton("Show Answer");
			assertTrue(solo.waitForText("Easy", 1, 1));
			solo.clickOnButton("Easy");

			long start = System.currentTimeMillis();
			assertTrue(solo.waitForDialogToClose(15000));

			tottime += rev.lastTime;
			Log.d(TAG, "Loop: " + j + " last: " + (System.currentTimeMillis() - start) + " real last: " + rev.lastTime + " avg: " + tottime/(j+1) + " real avg: " + rev.avgTime);
			if (j % 25 == 0) {
				Log.i(TAG, "Loop: " + j + " last: " + (System.currentTimeMillis() - start) + " real last: " + rev.lastTime + " avg: " + tottime/(j+1) + " real avg: " + rev.avgTime);
			}
		}
		Log.i(TAG, "Average: " + (tottime/((double)ANSCARDS)) + " app avg: " + rev.avgTime);
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
