package com.ichi2.libanki.test;

import java.util.Arrays;

import org.json.JSONException;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Sched;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

public class UndoTestCase extends InstrumentationTestCase {
	public UndoTestCase(String name) {
		setName(name);
	}
	
	@MediumTest
	public void test_review() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		try {
			d.getConf().put("counts", Sched.COUNT_REMAINING);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		d.reset();
		assertFalse(d.undoAvailable());
		// answer
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		Card c = d.getSched().getCard();
		assertTrue(c.getQueue() == 0);
		d.getSched().answerCard(c, 2);
		assertTrue(c.getLeft() == 1001);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 1, 0}));
		assertTrue(c.getQueue() == 1);
		// undo
		assertTrue(d.undoAvailable());
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		c.load();
		assertTrue(c.getQueue() == 0);
		assertTrue(c.getLeft() != 1001);
		assertFalse(d.undoAvailable());
		// we should be able to undo multiple answers too
		f.setitem("Front", "two");
		d.addNote(f);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 2);
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 2);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 2, 0}));
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 1, 0}));
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// DIFFERS FROM LIBANKI: AnkiDroid doesn't support undo for all ops
		// The ops that are undoable don't clear the review undos
	}
}
