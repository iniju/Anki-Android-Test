package com.ichi2.libanki.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.SpannableStringBuilder;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Sched;
import com.ichi2.libanki.Utils;

public class SchedTestCase extends InstrumentationTestCase {
	
	@MediumTest
	public void test_basics() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		d.reset();
		assertNull(d.getSched().getCard());
	}
	
	@MediumTest
	public void test_new() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		d.reset();
		assertTrue(d.getSched().getNewCount() == 0);
		// add a note
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		d.reset();
		assertTrue(d.getSched().getNewCount() == 1);
		// fetch it
		Card c = d.getSched().getCard();
		assertNotNull(c);
		assertTrue(c.getQueue() == 0);
		assertTrue(c.getType() == 0);
		// if we answer it, it should become a learn card
		long ts = Utils.intNow();
		d.getSched().answerCard(c, 1);
		assertTrue(c.getQueue() == 1);
		assertTrue(c.getType() == 1);
		assertTrue(c.getDue() >= ts);
		// the default order should ensure siblings are not seen together,
		// and should show all cards
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("Reverse");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		mm.save(m);
		f = d.newNote();
		f.setitem("Front", "2");
		f.setitem("Back", "2");
		d.addNote(f);
		f = d.newNote();
		f.setitem("Front", "3");
		f.setitem("Back", "3");
		d.addNote(f);
		d.reset();
		String[] qs = {"2", "3", "2", "3"};
		for (String n : qs) {
			c = d.getSched().getCard();
			assertTrue(c.getQuestion(false).contains(n));
			d.getSched().answerCard(c, 2);
		}
	}
	
	@MediumTest
	public void test_newLimits() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add some notes
		long g2 = d.getDecks().id("Default::foo");
		for (int i = 0; i < 30; ++i) {
			Note f = d.newNote();
			f.setitem("Front", Integer.toString(i));
			if (i > 4) {
				try {
					f.model().put("did", g2);
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}
			d.addNote(f);
		}
		// give the child deck a different configuration
		long c2 = d.getDecks().confId("new conf");
		d.getDecks().setConf(d.getDecks().get(g2), c2);
		d.reset();
		// both confs have defaulted to a limit of 20
		assertTrue(d.getSched().getNewCount() == 20);
		// first card we get comes from parent
		Card c = d.getSched().getCard();
		assertTrue(c.getDid() == 1);
		// limit the parent to 10 cards, meaning we get 10 in total
		JSONObject conf1 = d.getDecks().confForDid(1);
		try {
			conf1.getJSONObject("new").put("perDay", 10);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.reset();
		assertTrue(d.getSched().getNewCount() == 10);
		// if we limit child to 4, we should get 9
		JSONObject conf2 = d.getDecks().confForDid(g2);
		try {
			conf2.getJSONObject("new").put("perDay", 4);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.reset();
		assertTrue(d.getSched().getNewCount() == 9);
	}
	
	/*
	@MediumTest
	public void test_newOrder() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		JSONObject m = d.getModels().current();
		try {
			for (int i = 0; i < 50; ++i) {
				JSONObject t = d.getModels().newTemplate(m.toString());
				t.put("name", Integer.toString(i));
				t.put("qfmt", "{{Front}}");
				t.put("afmt", "{{Back}}");
				t.put("actv", i > 25);
				d.getModels().addTemplate(m, t);
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().save(m);
		Note f = d.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "2");
		// add first half
		d.addNote(f);
		// generate second half
		d.getDb().execute("update cards set did = random()");
		try {
			d.getConf().put("newPerDay", 100);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.reset();
		// cards should be sorted by id
		LinkedList<long[]> newqueue = d.getSched().getNewQueue();
		LinkedList<long[]> sorted = new LinkedList<long[]>(newqueue);
		Collections.sort(sorted, new LongListComparator());

		
	}
	*/
	
	public class DeckDueListComparator implements Comparator<Object[]> {
	    public int compare(Object[] o1, Object[] o2) {
			return ((String) o1[0]).compareTo((String) o2[0]);
	    }
	}
	
	@MediumTest
	public void test_newBoxes() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		d.reset();
		Card c = d.getSched().getCard();
		try {
			d.getSched()._cardConf(c).getJSONObject("new").put("delays", new JSONArray("[1, 2, 3, 4, 5]"));
			d.getSched().answerCard(c, 2);
			// should handle gracefully
			d.getSched()._cardConf(c).getJSONObject("new").put("delays", new JSONArray("[1]"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getSched().answerCard(c, 2);
	}
	
	@MediumTest
	public void test_learn() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add a note
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		// set as a learn card and rebuild queues
		d.getDb().execute("update cards set queue=0, type=0");
		d.reset();
		// sched.getCard should return it, since it's due in the past
		Card c = d.getSched().getCard();
		assertNotNull(c);
		try {
			d.getSched()._cardConf(c).getJSONObject("new").put("delays", new JSONArray("[0.5, 3, 10]"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		// fail it
		d.getSched().answerCard(c, 1);
		// it should have three reps left to graduation
		assertTrue(c.getLeft() == 3);
		// it should be due in 30 seconds
		long t = c.getDue() - Utils.intNow();
		assertTrue(t >= 25 && t <= 40);
		// pass it once
		d.getSched().answerCard(c, 2);
		// it should be due in 3 minutes
		t = c.getDue() - Utils.intNow();
		assertTrue(t == 179 || t == 180);
		assertTrue(c.getLeft() == 2);
		// check log is accurate
		Cursor cur = d.getDb().getDatabase().rawQuery("select * from revlog order by id desc", null);
		assertTrue(cur.moveToFirst());
		assertTrue(cur.getInt(3) == 2);
		assertTrue(cur.getInt(4) == -180);		
		assertTrue(cur.getInt(5) == -30);
		cur.close();
		// pass it again
		d.getSched().answerCard(c, 2);
		// it should be due in 10 minutes
		t = c.getDue() - Utils.intNow();
		assertTrue(t == 599 || t == 600);
		assertTrue(c.getLeft() == 1);
		// the next pass should graduate the card
		assertTrue(c.getQueue() == 1);
		assertTrue(c.getType() == 1);
		d.getSched().answerCard(c, 2);
		assertTrue(c.getQueue() == 2);
		assertTrue(c.getType() == 2);
		// should be due tomorrow, with an interval of 1
		assertTrue(c.getDue() == d.getSched().getToday() + 1);
		assertTrue(c.getIvl() == 1);
		// of normal removal
		c.setType(0);
		c.setQueue(1);
		d.getSched().answerCard(c, 3);
		assertTrue(c.getType() == 2);
		assertTrue(c.getQueue() == 2);
		assertTrue(c.getIvl() == 4);
		// revlog should have been updated each time
		assertTrue(d.getDb().queryScalar("select count() from revlog where type = 0") == 5);
		// now failed card handling
		c.setType(2);
		c.setQueue(1);
		c.setODue(123);
		d.getSched().answerCard(c, 3);
		assertTrue(c.getDue() == 123);
		assertTrue(c.getType() == 2);
		assertTrue(c.getQueue() == 2);
		// we should be able to remove manually, too
		c.setType(2);
		c.setQueue(1);
		c.setODue(321);
		c.flush();
		d.getSched().removeFailed();
		c.load();
		assertTrue(c.getQueue() == 2);
		assertTrue(c.getDue() == 321);
	}
	
	@MediumTest
	public void test_learn_collapsed() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add 2 notes
		Note f = d.newNote();
		f.setitem("Front", "1");
		d.addNote(f);
		f = d.newNote();
		f.setitem("Front", "2");
		d.addNote(f);
		// set as a learn card and rebuild queues
		d.getDb().execute("update cards set queue=0, type=0");
		d.reset();
		// should get '1' first
		Card c = d.getSched().getCard();
		assertTrue(c.getQuestion(false).endsWith("1"));
		// pass it so it's due in 10 minutes
		d.getSched().answerCard(c, 2);
		// get the other card
		c = d.getSched().getCard();
		assertTrue(c.getQuestion(false).endsWith("2"));
		// fail it so it's due in 1 minute
		d.getSched().answerCard(c, 1);
		// we shouldn't get the same card again
		c = d.getSched().getCard();
		assertFalse(c.getQuestion(false).endsWith("2"));
	}
	
	@MediumTest
	public void test_reviews() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add note
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		// set the card up as a review card, due 8 days ago
		Card c = f.cards().get(0);
		c.setType(2);
		c.setQueue(2);
		c.setDue(d.getSched().getToday() - 8);
		c.setFactor(2500);
		c.setReps(3);
		c.setLapses(1);
		c.setIvl(100);
		c.startTimer();
		c.flush();
		// save it for later use as well
		Card cardcopy = (Card) c.clone();
		// failing should put it in the learn queue with the default options
		////////////////////////////////////////////////////////////////////
		// different delay to new
		d.reset();
		try {
			d.getSched()._cardConf(c).getJSONObject("lapse").put("delays", new JSONArray("[2, 20]"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getSched().answerCard(c, 1);
		assertTrue(c.getQueue() == 1);
		// it should be due tomorrow, with an interval of 1
		assertTrue(c.getODue() == d.getSched().getToday() + 1);
		assertTrue(c.getIvl() == 1);
		// but because it's in the learn queue, its current due time should be in the future
		assertTrue(c.getDue() >= Utils.intNow());
		assertTrue((c.getDue() - Utils.intNow()) > 119);
		// factor should have been decremented
		assertTrue(c.getFactor() == 2300);
		// check counters
		assertTrue(c.getLapses() == 2);
		assertTrue(c.getReps() == 4);
		// check ests.
		assertTrue(d.getSched().nextIvl(c, 1) == 120);
		assertTrue(d.getSched().nextIvl(c, 2) == 20*60);
		// trye again with an ease of 2 instead
		///////////////////////////////////////
		c = cardcopy.clone();
		c.flush();
		d.getSched().answerCard(c, 2);
		assertTrue(c.getQueue() == 2);
		// the new interval should be (100 + 8/4) * 1.2 = 122
		assertTrue(c.getIvl() == 122);
		// factor should have been decremented
		assertTrue(c.getFactor() == 2350);
		// check counters
		assertTrue(c.getLapses() == 1);
		assertTrue(c.getReps() == 4);
		// ease 3
		/////////
		c = cardcopy.clone();
		c.flush();
		d.getSched().answerCard(c, 3);
		// the new interval should be (100 + 8/2) * 2.5 = 260
		assertTrue(c.getIvl() == 260);
		assertTrue(c.getDue() == d.getSched().getToday() + 260);
		// factor should have been left alone
		assertTrue(c.getFactor() == 2500);
		// ease 4
		/////////
		c = cardcopy.clone();
		c.flush();
		d.getSched().answerCard(c, 4);
		// the new interval should be (100 + 8) * 2.5 * 1.3= 351
		assertTrue(c.getIvl() == 351);
		assertTrue(c.getDue() == d.getSched().getToday() + 351);
		// factor should have been increased
		assertTrue(c.getFactor() == 2650);
		// leech handling
		/////////////////
		c = cardcopy.clone();
		c.setLapses(7);
		c.flush();
		// Leech hook not implemented in AnkiDroid
		d.getSched().answerCard(c, 1);
		assertTrue(c.getQueue() == -1);
		c.load();
		assertTrue(c.getQueue() == -1);
	}
	
	@MediumTest
	public void test_overdue_lapse() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add note
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		// simulate a review that was lapsed and is now due for its normal review
		Card c = f.cards().get(0);
		c.setType(2);
		c.setQueue(1);
		c.setDue(-1);
		c.setODue(-1);
		c.setFactor(2500);
		c.setLeft(2);
		c.setIvl(0);
		c.flush();
		d.getSched().setClearOverdue(false);
		// checkpoint
		d.save();
		d.getDb().getDatabase().beginTransaction();
		d.getSched().reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 2, 0}));
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		// it should be due tomorrow
		assertTrue(c.getDue() == d.getSched().getToday() + 1);
		// revert to before
		d.rollback();
		d.getSched().setClearOverdue(true);
		// with the default settings, the overdue card should be removed from the learning queue
		d.getSched().reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 1}));
	}
	
	@MediumTest
	public void test_finished() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// nothing due
		String finishedMsg = ((SpannableStringBuilder) d.getSched().finishedMsg(getInstrumentation().getTargetContext())).toString();
		MoreAsserts.assertContainsRegex(Pattern.quote("Congratulations"), finishedMsg);
		MoreAsserts.assertNotContainsRegex(Pattern.quote("limit"), finishedMsg);
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		// have a new card
		finishedMsg = ((SpannableStringBuilder) d.getSched().finishedMsg(getInstrumentation().getTargetContext())).toString();
		MoreAsserts.assertContainsRegex(Pattern.quote("new cards available"), finishedMsg);
		// turn it into a review
		d.reset();
		Card c = f.cards().get(0);
		c.startTimer();
		d.getSched().answerCard(c, 3);
		// nothing should be due tomorrow, as it's due in a week
		finishedMsg = ((SpannableStringBuilder) d.getSched().finishedMsg(getInstrumentation().getTargetContext())).toString();
		MoreAsserts.assertContainsRegex(Pattern.quote("Congratulations"), finishedMsg);
		MoreAsserts.assertNotContainsRegex(Pattern.quote("limit"), finishedMsg);
	}
	
	@MediumTest
	public void test_nextIvl() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		d.reset();
		JSONObject conf = d.getDecks().confForDid(1);
		try {
			conf.getJSONObject("new").put("delays", new JSONArray("[0.5, 3, 10]"));
			conf.getJSONObject("lapse").put("delays", new JSONArray("[1, 5, 9]"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		Card c = d.getSched().getCard();
		// new cards
		////////////
		assertTrue(d.getSched().nextIvl(c, 1) == 30);
		assertTrue(d.getSched().nextIvl(c, 2) == 180);
		assertTrue(d.getSched().nextIvl(c, 3) == 4 * 86400);
		d.getSched().answerCard(c, 1);
		// cards in learning
		////////////////////
		assertTrue(d.getSched().nextIvl(c, 1) == 30);
		assertTrue(d.getSched().nextIvl(c, 2) == 180);
		assertTrue(d.getSched().nextIvl(c, 3) == 4 * 86400);
		d.getSched().answerCard(c, 2);
		assertTrue(d.getSched().nextIvl(c, 1) == 30);
		assertTrue(d.getSched().nextIvl(c, 2) == 600);
		assertTrue(d.getSched().nextIvl(c, 3) == 4 * 86400);
		d.getSched().answerCard(c, 2);
		// normal graduation is tomorrow
		assertTrue(d.getSched().nextIvl(c, 2) == 1 * 86400);
		assertTrue(d.getSched().nextIvl(c, 3) == 4 * 86400);
		// lapsed cards
		///////////////
		c.setType(2);
		c.setIvl(100);
		c.setFactor(2500);
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		assertTrue(d.getSched().nextIvl(c, 2) == 100 * 86400);
		assertTrue(d.getSched().nextIvl(c, 3) == 100 * 86400);
		// review cards
		///////////////
		c.setQueue(2);
		c.setIvl(100);
		c.setFactor(2500);
		// failing it should put it at 60s
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		// or 1 day if relearn is false
		try {
			d.getSched()._cardConf(c).getJSONObject("lapse").put("delays", new JSONArray());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		assertTrue(d.getSched().nextIvl(c, 1) == 1 * 86400);
		// 100 * 1.2 * 86400 = 10368000.0
		assertTrue(d.getSched().nextIvl(c, 2) == 10368000);
		// 100 * 2.5 * 86400 = 21600000.0
		assertTrue(d.getSched().nextIvl(c, 3) == 21600000);
		// 100 * 2.5 * 1.3 * 86400 = 28080000.0
		assertTrue(d.getSched().nextIvl(c, 4) == 28080000);
		assertTrue(d.getSched().nextIvlStr(c, 4).compareTo("10.8 months") == 0);
	}
	
	@MediumTest
	public void test_misc() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		Card c = f.cards().get(0);
		// burying
		d.getSched().buryNote(c.getNid());
		d.reset();
		assertNull(d.getSched().getCard());
		d.getSched().onClose();
		d.reset();
		assertNotNull(d.getSched().getCard());
	}

	@MediumTest
	public void test_suspend() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		Card c = f.cards().get(0);
		// suspending
		d.reset();
		assertNotNull(d.getSched().getCard());
		d.getSched().suspendCards(new long[]{c.getId()});
		d.reset();
		assertNull(d.getSched().getCard());
		// unsuspending
		d.getSched().unsuspendCards(new long[]{c.getId()});
		d.reset();
		assertNotNull(d.getSched().getCard());
		// should cope with rev cards being relearnt
		c.setDue(0);
		c.setIvl(100);
		c.setType(2);
		c.setQueue(2);
		c.flush();
		d.reset();
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 1);
		assertTrue(c.getDue() >= Utils.intNow());
		assertTrue(c.getQueue() == 1);
		assertTrue(c.getType() == 2);
		d.getSched().suspendCards(new long[]{c.getId()});
		d.getSched().unsuspendCards(new long[]{c.getId()});
		c.load();
		assertTrue(c.getQueue() == 2);
		assertTrue(c.getType() == 2);
		assertTrue(c.getDue() == 1);
		// should cope with cards in cram decks
		c.setDue(1);
		c.flush();
		d.getDecks().newDyn("tmp");
		d.getSched().rebuildDyn();
		c.load();
		assertTrue(c.getDue() != 1);
		assertTrue(c.getDid() != 1);
		d.getSched().suspendCards(new long[]{c.getId()});
		c.load();
		assertTrue(c.getDue() == 1);
		assertTrue(c.getDid() == 1);
	}
	
	@MediumTest
	public void test_cram() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		Card c = f.cards().get(0);
		c.setIvl(100);
		c.setType(2);
		c.setQueue(2);
		// due in 25 days, so it's been waiting 75 days
		c.setDue(d.getSched().getToday() + 25);
		c.setMod(1);
		c.setFactor(2500);
		c.startTimer();
		c.flush();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		Card cardcopy = c.clone();
		// create a dynamic deck and refresh it
		long did = d.getDecks().newDyn("Cram");
		d.getSched().rebuildDyn(did);
		d.reset();
		// should appear as new in the deck list
		List<Object[]> sorted = d.getSched().deckDueList(Sched.DECK_INFORMATION_SIMPLE_COUNTS);
		Collections.sort(sorted, new DeckDueListComparator());
		// DIFFERS FROM LIBANKI: AnkiDroid differs here because deckDueList
		// returns [deckname, did, new, lrn, rev] instead of [deckname, did, rev, lrn, new]
		assertTrue((Integer)(sorted.get(0)[2]) == 1);
		// and should appear in the counts
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		// grab it and check estimates
		c = d.getSched().getCard();
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		assertTrue(d.getSched().nextIvl(c, 2) == 600);
		assertTrue(d.getSched().nextIvl(c, 3) == 138 * 60 * 60 * 24);
		d.getSched().answerCard(c, 2);
		// elapsed time was 75 days
		// factor = 2.5 + 1.2 / 2 = 1.85
		// int(75 * 1.85) = 138
		assertTrue(c.getIvl() == 138);
		assertTrue(c.getODue() == 138);
		assertTrue(c.getQueue() == 1);
		// should be logged as a cram rep
		assertTrue(d.getDb().queryScalar("select type from revlog order by id desc limit 1") == 3);
		// check ivls again
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		assertTrue(d.getSched().nextIvl(c, 2) == 138 * 60 * 60 * 24);
		assertTrue(d.getSched().nextIvl(c, 3) == 138 * 60 * 60 * 24);
		// when it graduates, due is updated
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 2);
		assertTrue(c.getIvl() == 138);
		assertTrue(c.getDue() == 138);
		assertTrue(c.getQueue() == 2);
		// and it will have moved back to the previous deck
		assertTrue(c.getDid() == 1);
		// cram the deck again
		d.getSched().rebuildDyn(did);
		d.reset();
		c = d.getSched().getCard();
		// check ivls again - passing should be idempontent
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		assertTrue(d.getSched().nextIvl(c, 2) == 600);
		assertTrue(d.getSched().nextIvl(c, 3) == 138 * 60 * 60 * 24);
		d.getSched().answerCard(c, 2);
		assertTrue(c.getIvl() == 138);
		assertTrue(c.getODue() == 138);
		// fail
		d.getSched().answerCard(c, 1);
		assertTrue(d.getSched().nextIvl(c, 1) == 60);
		assertTrue(d.getSched().nextIvl(c, 2) == 600);
		assertTrue(d.getSched().nextIvl(c, 3) == 86400);
		// delete the deck, returning the card mid-study
		d.getDecks().rem(d.getDecks().selected());
		assertTrue(d.getSched().deckDueList(Sched.DECK_INFORMATION_SIMPLE_COUNTS).size() == 1);
		c.load();
		assertTrue(c.getIvl() == 1);
		assertTrue(c.getDue() == d.getSched().getToday() + 1);
		// make it due
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		c.setDue(0);
		c.setIvl(100);
		c.flush();
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 1}));
		// cram again
		did = d.getDecks().newDyn("Cram");
		// if cramRev is false, it's placed in the review queue instead
		try {
			d.getDecks().get(did).put("cramRev", false);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getSched().rebuildDyn(did);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 1}));
		// add a sibling so we can test minSpace, etc
		Card c2 = cardcopy.clone();
		c2.setId(123);
		c2.setOrd(1);
		c2.setDue(325);
		c2.setCol(c.getCol());
		c2.flush();
		// should be able to answer it
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 4);
		// it should have been moved back to original deck
		assertTrue(c.getDid() == 1);
	}
	
	@MediumTest
	public void test_cram_rem() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		d.addNote(f);
		long oldDue = f.cards().get(0).getDue();
		long did = d.getDecks().newDyn("Cram");
		d.getSched().rebuildDyn(did);
		d.reset();
		Card c = d.getSched().getCard();
		d.getSched().answerCard(c, 2);
		// answering the card will put it in the learning queue
		assertTrue((c.getType() == c.getQueue()) && (c.getQueue() == 1));
		assertTrue(c.getDue() != oldDue);
		// if we terminate cramming prematurely it should be set back to new
		d.getSched().remDyn(did);
		c.load();
		assertTrue((c.getType() == c.getQueue()) && (c.getQueue() == 0));
		assertTrue(c.getDue() == oldDue);
	}
	
	@MediumTest
	public void test_adjIvl() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add two more templates and set second active
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("Reverse");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		mm.save(m);
		t = d.getModels().newTemplate(m.toString());
		try {
			t.put("name", "f2");
			t.put("qfmt", "{{Front}}");
			t.put("afmt", "{{Back}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().addTemplate(m, t);
		t = d.getModels().newTemplate(m.toString());
		try {
			t.put("name", "f3");
			t.put("qfmt", "{{Front}}");
			t.put("afmt", "{{Back}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().addTemplate(m, t);
		d.getModels().save(m);
		// create new note; it should have 4 cards
		Note f = d.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "1");
		d.addNote(f);
		assertTrue(d.cardCount() == 4);
		d.reset();
		// immediately remove first; it should get ideal ivl
		Card c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 4);
		// with the default settings, second card should be -1
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 3);
		// and third +1
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 5);
		// fourth exceeds default settings, so gets ideal again
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 4);
		// try again with another note
		f = d.newNote();
		f.setitem("Front", "2");
		f.setitem("Back", "2");
		d.addNote(f);
		d.reset();
		// set a minSpacing of 0
		JSONObject conf = d.getSched()._cardConf(c);
		try {
			conf.getJSONObject("rev").put("minSpace", 0);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		// first card gets ideal
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 4);
		// and second too, because it's below the threshold
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 4);
		// if we increase the ivl minSpace isn't needed
		try {
			conf.getJSONObject("new").getJSONArray("ints").put(1, 20);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		// ideal..
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 20);
		// adjusted
		c = d.getSched().getCard();
		d.getSched().answerCard(c, 3);
		assertTrue(c.getIvl() == 19);
	}
	
	@MediumTest
	public void test_ordCycle() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		// add two more templates and set second active
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("Reverse");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		t = d.getModels().newTemplate(m.toString());
		try {
			t.put("name", "f2");
			t.put("qfmt", "{{Front}}");
			t.put("afmt", "{{Back}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().addTemplate(m, t);
		mm.save(m);
		// create a new note; it should have 3 cards
		Note f = d.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "1");
		d.addNote(f);
		assertTrue(d.cardCount() == 3);
		d.reset();
		// ordinals should arrive in order
		assertTrue(d.getSched().getCard().getOrd() == 0);
		assertTrue(d.getSched().getCard().getOrd() == 1);
		assertTrue(d.getSched().getCard().getOrd() == 2);
	}
	
	@MediumTest
	public void test_counts_idx() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertNotNull(d);
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "two");
		d.addNote(f);
		d.reset();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{1, 0, 0}));
		Card c = d.getSched().getCard();
		// counter's been decremented but idx indicates 1
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		assertTrue(d.getSched().countIdx(c) == 0);
		// answer to move to learn queue
		d.getSched().answerCard(c, 1);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 2, 0}));
		// fetching again will decrement the count
		c = d.getSched().getCard();
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 0, 0}));
		assertTrue(d.getSched().countIdx(c) == 1);
		// answering should add it back again
		d.getSched().answerCard(c, 1);
		assertTrue(Arrays.equals(d.getSched().counts(), new int[]{0, 2, 0}));
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
