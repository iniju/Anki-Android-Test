package com.ichi2.libanki.test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.async.DeckTask;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Sched;
import com.ichi2.libanki.Utils;

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
	
	/**
	 * Listener class for Dismiss DeckTasks
	 */
	private class DismissTaskListener implements DeckTask.TaskListener {
		public Card mNextCard;
		public void onPreExecute() {}
		public void onProgressUpdate(DeckTask.TaskData... values) {
			mNextCard = values[0].getCard();
		}
		public void onPostExecute(DeckTask.TaskData result) {}
    };
    
    /**
     * Wrapper for {@link DeckTask} operations of type DeckTask.TASK_TYPE_DISMISS_NOTE
     * @param data A {@link DeckTask.TaskData} object describing the type of operation.
     * @param wait How long to wait for the AsyncTask to finish in seconds.
     * @return The next Card scheduled, if any. This operation automatically gets the next card.
     */
    private Card doCardTask(int type, DeckTask.TaskData data, int wait) {
    	DismissTaskListener mDismissCardHandler = new DismissTaskListener();
		DeckTask task = DeckTask.launchDeckTask(type, mDismissCardHandler, data);
		try {
			assertTrue(task.get(wait, TimeUnit.SECONDS).getBoolean());
			return mDismissCardHandler.mNextCard;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
    }
	
	// NOT IN LIBANKI
	@MediumTest
	public void test_undo_edit() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("rev");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		mm.save(m, true);
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		assertTrue(f.cards().size() == 2);
		Card c1 = d.getSched().getCard();
		c1.note().setitem("Front", "one-one");
		assertFalse(d.undoAvailable());
		// edit
		doCardTask(DeckTask.TASK_TYPE_UPDATE_FACT, new DeckTask.TaskData(d.getSched(), c1, true), 5);
		assertTrue(d.undoAvailable());
		// the edited card is modified
		c1.load();
		assertTrue(c1.getQuestion(false).contains("one-one"));
		// the update should have modified all the note's cards
		f.load();
		assertTrue(f.cards().get(1).getAnswer(false).contains("one-one"));
		// undo
		d.undo();
		assertFalse(d.undoAvailable());
		// refresh cache
		f.load();
		// both cards should be undone
		assertTrue(Utils.stripHTML(f.cards().get(0).getQuestion(false)).equals("one"));
		assertTrue(Utils.stripHTML(f.cards().get(1).getAnswer(false)).equals("one"));
	}
	
	// NOT IN LIBANKI
	@MediumTest
	public void test_undo_dismiss() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("rev");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		mm.save(m, true);
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		// Undo Suspend Card
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// suspend first card
		Card c1 = d.getSched().getCard();
		assertTrue(c1.getQueue() == 0);
		Card c2 = doCardTask(DeckTask.TASK_TYPE_DISMISS_NOTE, new DeckTask.TaskData(d.getSched(), c1, 1), 5);
		// The next scheduled card should be different
		assertTrue(c2.getId() != c1.getId());
		c1.load();
		// the old should be in queue -1
		assertTrue(c1.getQueue() == -1);
		d.reset();
		// confirm the new counts
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		// undo the card suspension
		d.undo();
		c1.load();
		// The card is back in queue 0
		assertTrue(c1.getQueue() == 0);
		d.reset();
		// And the counts are as before
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// The next scheduled card is c1 again
		Card c = d.getSched().getCard();
		assertTrue(c.getId() == c1.getId());
		d.reset();
		// Undo Suspend Note
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		c = doCardTask(DeckTask.TASK_TYPE_DISMISS_NOTE, new DeckTask.TaskData(d.getSched(), c, 2), 5000);
		// Should have no more cards left in the deck and null returned
		assertTrue(c == null);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		assertTrue(f.cards().get(0).getQueue() == -1);
		assertTrue(f.cards().get(1).getQueue() == -1);
		// undo
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// mix review and suspension undos
		c1 = d.getSched().getCard();
		c2 = doCardTask(DeckTask.TASK_TYPE_DISMISS_NOTE, new DeckTask.TaskData(d.getSched(), c1, 1), 5);
		d.getSched().answerCard(c2, 2);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 1, 0}));
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// Undo Bury Note
		c1 = d.getSched().getCard();
		d.getSched().answerCard(c1, 2);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 1, 0}));
		c2 = d.getSched().getCard();
		c = doCardTask(DeckTask.TASK_TYPE_DISMISS_NOTE, new DeckTask.TaskData(d.getSched(), c2, 0), 5);
		// Should have no more cards left in the deck and null returned
		//assertTrue(c == null);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		// the queues should now be -2
		assertTrue(f.cards().get(0).getQueue() == -2);
		assertTrue(f.cards().get(1).getQueue() == -2);
		// undo the note suspension
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 1, 0}));
		c1.load();
		c2.load();
		assertTrue(c1.getQueue() == 1);
		assertTrue(c2.getQueue() == 0);
		c = d.getSched().getCard();
		// the next card should be c2 again
		assertTrue(c2.getId() == c.getId());
		// Undo Delete Note
		// add another note for good measure
		f = d.newNote();
		f.setitem("Front", "foo");
		f.setitem("Back", "bar");
		d.addNote(f);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{3, 1, 0}));
		c1 = d.getSched().getCard();
		doCardTask(DeckTask.TASK_TYPE_DISMISS_NOTE, new DeckTask.TaskData(d.getSched(), c1, 3), 5);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{2, 0, 0}));
		// undo
		d.undo();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{3, 1, 0}));
	}
	
	// NOT IN LIBANKI
	@MediumTest
	public void test_undo_mark() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		d.reset();
		assertFalse(f.hasTag("marked"));
		// mark
		doCardTask(DeckTask.TASK_TYPE_MARK_CARD, new DeckTask.TaskData(d.getSched(), f.cards().get(0), 0), 5);
		f.load();
		assertTrue(f.hasTag("marked"));
		// undo
		d.undo();
		f.load();
		assertFalse(f.hasTag("marked"));
	}

}
