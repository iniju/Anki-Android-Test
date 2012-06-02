package com.ichi2.libanki.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.sync.LocalServer;
import com.ichi2.libanki.sync.Syncer;

public class SyncTestCase extends InstrumentationTestCase {

	Collection deck1;
	Collection deck2;
	LocalServer server;
	Syncer client;

	@MediumTest
	public void setup_basic() {
		deck1 = Shared.getEmptyDeck(getInstrumentation().getContext());
		// add a note to deck 1
		Note f = deck1.newNote();
		f.setitem("Front", "foo");
		f.setitem("Back", "bar");
		f.setTagsFromStr("foo");
		deck1.addNote(f);
		// answer it
		deck1.reset();
		deck1.getSched().answerCard(deck1.getSched().getCard(), 4);
		// repeat for deck2
		deck2 = Shared.getEmptyDeck(getInstrumentation().getContext(), true);
		f = deck2.newNote();
		f.setitem("Front", "bar");
		f.setitem("Back", "bar");
		f.setTagsFromStr("bar");
		deck2.addNote(f);
		deck2.reset();
		deck2.getSched().answerCard(deck2.getSched().getCard(), 4);
		// start with same schema and sync time
		deck1.setScm(0);
		deck2.setScm(0);
		// and same mot time, so sync does nothing
		long t = Utils.intNow(1000);
		deck1.save(t);
		deck2.save(t);
		server = new LocalServer(deck2);
		client = new Syncer(deck1, server);
	}

	public void setup_modified() {
		setup_basic();
		// mark deck1 as changed
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		deck1.setMod();
		deck1.save();
	}

	public void test_nochange() {
		setup_basic();
		assertTrue(((String)client.sync()[0]).compareTo("noChanges") == 0);
	}

	public void test_changedSchema() {
		setup_modified();
		deck1.setScm(deck1.getScm()+1);
		deck1.setMod();
		assertTrue(((String)client.sync()[0]).compareTo("fullSync") == 0);
	}

	public void test_sync() {
		setup_modified();
		check(1);
		int origUsn = deck1.usn();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		// last sync times and mod times should agree
		assertTrue(deck1.getMod() == deck2.getMod());
		assertTrue(deck1.getUsnForSync() == deck2.getUsnForSync());
		assertTrue(deck1.getMod() == deck2.getLs());
		assertTrue(deck1.getUsnForSync() != origUsn);
		// because everything was created separately it will be merged in. in actual use, we use a full sync to ensure a common starting point.
		check(2);
		// repeating it does nothing
		assertTrue(((String)client.sync()[0]).compareTo("noChanges") == 0);
		// if we bump mod time, the decks will sync but should remain the same.
		deck1.setMod();
		deck1.save();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		check(2);
	}
	private void check(int num) {
		for (Collection d : new Collection[]{deck1, deck2}) {
			for (String t : new String[]{"revlog", "notes", "cards"}) {
				assertTrue(d.getDb().queryScalar("SELECT count() FROM " + t) == num);
			}
			assertTrue(d.getModels().all().size() == num * 2);
			// the default deck and config have an id of 1, so always 1
			assertTrue(d.getDecks().all().size() == 1);
			assertTrue(d.getDecks().getDconf().size() == 1);
			assertTrue(d.getTags().all().length == num);
		}
	}

	public void test_models() {
		test_sync();
		// update model one
		JSONObject cm = deck1.getModels().current();
		try {
			cm.put("name", "new");
			Thread.sleep(1000);
			deck1.getModels().save(cm);
			deck1.save();
			assertTrue(deck2.getModels().get(cm.getLong("id")).getString("name").compareTo("Basic") == 0);
			assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
			assertTrue(deck2.getModels().get(cm.getLong("id")).getString("name").compareTo("new") == 0);
			// deleting triggers a full sync
			deck1.setScm(0);
			deck2.setScm(0);
			deck1.getModels().rem(cm);
			deck1.save();
			assertTrue(((String)client.sync()[0]).compareTo("fullSync") == 0);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void test_notes() {
		test_sync();
		// modifications should be synced
		long nid = deck1.getDb().queryLongScalar("SELECT id FROM notes");
		Note note = deck1.getNote(nid);
		assertTrue(note.getitem("Front").compareTo("abc") != 0);
		note.setitem("Front", "abc");
		note.flush();
		deck1.save();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(deck2.getNote(nid).getitem("Front").compareTo("abc") == 0);
		// deletions too
		assertTrue(deck1.getDb().queryLongScalar("SELECT 1 FROM notes WHERE id = " + nid, false) != 0);
		deck1.remNotes(new long[]{nid});
		deck1.save();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(deck1.getDb().queryLongScalar("SELECT 1 FROM notes WHERE id = " + nid, false) == 0);
		assertTrue(deck2.getDb().queryLongScalar("SELECT 1 FROM notes WHERE id = " + nid, false) == 0);
	}

	public void test_cards() {
		test_sync();
		// modifications should be synced
		long nid = deck1.getDb().queryLongScalar("SELECT id FROM notes");
		Note note = deck1.getNote(nid);
		Card card = note.cards().get(0);
		// answer the card locally
		card.startTimer();
		deck1.getSched().answerCard(card, 4);
		assertTrue(card.getReps() == 2);
		deck1.save();
		assertTrue(deck2.getCard(card.getId()).getReps() == 1);
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(deck2.getCard(card.getId()).getReps() == 2);
		// if it's modified on both sides, later mod time should win
		for (Collection[] test : new Collection[][] {new Collection[]{deck1, deck2}, new Collection[]{deck2, deck1}}) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			Card c = test[0].getCard(card.getId());
			c.setReps(5);
			c.flush();
			test[0].save();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			c = test[1].getCard(card.getId());
			c.setReps(3);
			c.flush();
			test[1].save();
			assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
			assertTrue(test[1].getCard(card.getId()).getReps() == 3);
			assertTrue(test[0].getCard(card.getId()).getReps() == 3);
		}
		// removals should work too
		deck1.remCards(new long[]{card.getId()});
		deck1.save();
		assertTrue(deck2.getDb().queryLongScalar("SELECT 1 FROM cards WHERE id = " + card.getId(), false) != 0);
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(deck2.getDb().queryLongScalar("SELECT 1 FROM cards WHERE id = " + card.getId(), false) == 0);
	}

	public void test_tags() {
		test_sync();
		assertTrue(Arrays.equals(deck1.getTags().all(), deck2.getTags().all()));
		ArrayList<String> list = new ArrayList<String>();
		list.add("abc");
		deck1.getTags().register(list);
		ArrayList<String> list2 = new ArrayList<String>();
		list2.add("xyz");
		deck2.getTags().register(list2);
		assertTrue(!Arrays.equals(deck1.getTags().all(), deck2.getTags().all()));
		deck1.save();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		deck2.save();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(Arrays.equals(deck1.getTags().all(), deck2.getTags().all()));
	}

	public void test_decks() {
		test_sync();
		assertTrue(deck1.getDecks().all().size() == 1);
		assertTrue(deck1.getDecks().all().size() == deck2.getDecks().all().size());
		deck1.getDecks().id("new");
		assertTrue(deck1.getDecks().all().size() != deck2.getDecks().all().size());
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		deck2.getDecks().id("new2");
		deck1.save();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		deck2.save();
		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
		assertTrue(Arrays.equals(deck1.getTags().all(), deck2.getTags().all()));
		assertTrue(deck1.getDecks().all().size() == deck2.getDecks().all().size());
		assertTrue(deck1.getDecks().all().size() == 3);
		try {
			assertTrue(deck1.getDecks().confForDid(1).getInt("maxTaken") == 60);
			deck2.getDecks().confForDid(1).put("maxTaken", 30);
			deck2.getDecks().save(deck2.getDecks().confForDid(1));
			deck2.save();
			assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
			assertTrue(deck1.getDecks().confForDid(1).getInt("maxTaken") == 30);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public void test_conf() {
		test_sync();
		try {
			assertTrue(deck2.getConf().getLong("curDeck") == 1);
			deck1.getConf().put("curDeck", 2);
			Thread.sleep(100);
			deck1.setMod();
			deck1.save();
			assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
			assertTrue(deck2.getConf().getLong("curDeck") == 2);			
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

//	public void test_threeway() {
////		test_sync();
////		deck1.close(false);
//		// TODO
//	}

	// test_speed
	
}
