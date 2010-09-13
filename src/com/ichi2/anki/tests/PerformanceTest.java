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

import java.util.ArrayList;
import java.lang.System;

public class PerformanceTest extends ActivityInstrumentationTestCase2<StudyOptions> {

	private static final String TAG = "AnkiDroidTest";
	private static final String DECK = "slow-normal";

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
		ProgressBar pg;
		boolean allDone = false;
		while (!allDone) {
			allDone = true;
			for (int i = 0; i < lv.getChildCount(); i++) {
				pg = (ProgressBar)(((LinearLayout)(((LinearLayout)(lv.getChildAt(i))).getChildAt(1))).getChildAt(2));
				if (pg.getVisibility() == View.VISIBLE) {
					allDone = false;
				}
			}
		}
		assertTrue(solo.waitForText(deck));
		solo.clickOnText(deck);
		assertTrue(solo.waitForActivity("StudyOptions", 60000));

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
		Log.i(TAG, "Start reviewing deck " + DECK " - 100 iterations.");
		solo.clickOnButton("Start Reviewing");
		assertTrue(solo.waitForActivity("Reviewer", 5));
		Reviewer rev = (Reviewer)(solo.getCurrentActivity());
		long tottime = 0;

		for (int j = 0; j < 100; j++) {
			assertTrue(solo.waitForText("Show Answer"));
			solo.clickOnButton("Show Answer");
			assertTrue(solo.waitForText("Easy", 1, 1));
			solo.clickOnButton("Easy");

			long start = System.currentTimeMillis();
			assertTrue(solo.waitForDialogToClose(15000));

			tottime += rev.lastTime;
			Log.i(TAG, "Loop: " + j + " last: " + (System.currentTimeMillis() - start) + " real last: " + rev.lastTime +
					" avg: " + tottime/(j+1) + " real avg: " + rev.avgTime);
		}
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
