//package com.ichi2.libanki.test;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.regex.Pattern;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import android.database.Cursor;
//import android.test.InstrumentationTestCase;
//import android.test.MoreAsserts;
//import android.test.suitebuilder.annotation.MediumTest;
//import android.text.SpannableStringBuilder;
//
//import com.ichi2.libanki.Card;
//import com.ichi2.libanki.Collection;
//import com.ichi2.libanki.Models;
//import com.ichi2.libanki.Note;
//import com.ichi2.libanki.Sched;
//import com.ichi2.libanki.Utils;
//import com.ichi2.libanki.sync.HttpSyncer;
//import com.ichi2.libanki.sync.LocalServer;
//import com.ichi2.libanki.sync.Syncer;
//import com.ichi2.libanki.sync.SyncerSuperclass;
//
//public class SyncTestCase extends InstrumentationTestCase {
//
//	Collection deck1;
//	Collection deck2;
//	SyncerSuperclass server;
//	Syncer client;
//
//	@MediumTest
//	public void setup_basic() {
//		deck1 = Shared.getEmptyDeck(getInstrumentation().getContext());
//		// add a note to deck 1
//		Note f = deck1.newNote();
//		f.setitem("Front", "foo");
//		f.setitem("Back", "bar");
//		f.setTagsFromStr("foo");
//		deck1.addNote(f);
//		// answer it
//		deck1.reset();
//		deck1.getSched().answerCard(deck1.getSched().getCard(), 4);
//		// repeat for deck2
//		deck2 = Shared.getEmptyDeck(getInstrumentation().getContext());
//		f = deck2.newNote();
//		f.setitem("Front", "bar");
//		f.setitem("Back", "bar");
//		f.setTagsFromStr("bar");
//		deck2.addNote(f);
//		deck2.reset();
//		deck2.getSched().answerCard(deck2.getSched().getCard(), 4);
//		// start with same schema and sync time
//		deck1.setScm(0);
//		deck2.setScm(0);
//		// and same mot time, so sync does nothing
//		long t = Utils.intNow(1000);
//		deck1.save(t);
//		deck2.save(t);
//		server = new LocalServer(deck2);
//		client = new Syncer(deck1, server);
//	}
//
//	public void setup_modified() {
//		setup_basic();
//		// mark deck1 as changed
//		try {
//			Thread.sleep(1);
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//		deck1.setMod();
//		deck1.save();
//	}
//
//	public void test_nochange() {
//		assertTrue(((String)client.sync()[0]).compareTo("noChanges") == 0);
//	}
//
//	public void test_changedSchema() {
//		deck1.setScm(deck1.getScm()+1);
//		deck1.setMod();
//		assertTrue(((String)client.sync()[0]).compareTo("fullSync") == 0);
//	}
//
//	public void test_sync() {
//		check(1);
//		int origUsn = deck1.usn();
//		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
//		// last sync times and mod times should agree
//		assertTrue(deck1.getMod() == deck2.getMod());
//		assertTrue(deck1.getUsnForSync() == deck2.getUsnForSync());
//		assertTrue(deck1.getMod() == deck2.getLs());
//		assertTrue(deck1.getUsnForSync() != origUsn);
//		// because everything was created separately it will be merged in. in actual use, we use a full sync to ensure a common starting point.
//		check(2);
//		// repeating it does nothing
//		assertTrue(((String)client.sync()[0]).compareTo("noChanges") == 0);
//		// if we bump mod time, the decks will sync but should remain the same.
//		deck1.setMod();
//		deck1.save();
//		assertTrue(((String)client.sync()[0]).compareTo("success") == 0);
//		check(2);	
//	}
//	private void check(int num) {
//		for (Collection d : new Collection[]{deck1, deck2}) {
//			for (String t : new String[]{"revlog", "notes", "cards"}) {
//				assertTrue(d.getDb().queryScalar("SELECT count() FROM " + t) == num);
//			}
//			assertTrue(d.getModels().all().size() == num * 2);
//			// the default deck and config have an id of 1, so always 1
//			assertTrue(d.getDecks().all().size() == 1);
//			assertTrue(d.getDecks().getDconf().size() == 1);
//			assertTrue(d.getTags().all().length == num);
//		}
//	}
//
//}
