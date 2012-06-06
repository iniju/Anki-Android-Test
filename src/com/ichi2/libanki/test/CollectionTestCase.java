package com.ichi2.libanki.test;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;

public class CollectionTestCase extends InstrumentationTestCase {
	CollectionTestCase(String name) {
		setName(name);
	}
	
	@MediumTest
	public void test_noteAddDelete() {
		try {
			Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
			// add a note
			Note f = deck.newNote();
			f.setitem("Front", "one");
			f.setitem("Back", "two");
			int n = deck.addNote(f);
			assertTrue(n == 1);
			// test multiple cards -- add another template
			JSONObject m = deck.getModels().current();
			Models mm = deck.getModels();
			JSONObject t = mm.newTemplate("Reverse");
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
			mm.addTemplate(m,  t);
			mm.save(m);
			// the default save doesn't generate cards
			assertTrue(deck.cardCount() == 1);
			// but when templates are edited such as in the card layout screen, it
		    // should generate cards on close
			mm.save(m, true);
			assertTrue(deck.cardCount() == 2);
			// creating new notes should use both cards
			f = deck.newNote();
			f.setitem("Front", "three");
			f.setitem("Back", "four");
			n = deck.addNote(f);
			assertTrue(n == 2);
			assertTrue(deck.cardCount() == 4);
			// check q/a generation
			Card c0 = f.cards().get(0);
			assertTrue(c0.getQuestion(false).contains("three"));
			// it should not be a duplicate
			assertTrue(f.dupeOrEmpty() == 0);
			// now let's make a duplicate
			Note f2 = deck.newNote();
			f2.setitem("Front", "one");
			f2.setitem("Back", "");
			assertTrue(f2.dupeOrEmpty() != 0);
			// empty first field should not be permitted either
			f2.setitem("Front", " ");
			assertTrue(f2.dupeOrEmpty() != 0);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@MediumTest
	public void test_fieldChecksum() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "new");
		f.setitem("Back", "new2");
		deck.addNote(f);
		assertTrue(deck.getDb().queryLongScalar("SELECT csum FROM notes") == Long.valueOf("c2a6b03f", 16));
		// changing the val should change the checksum
		f.setitem("Front", "newx");
		f.flush();
		assertTrue(deck.getDb().queryLongScalar("SELECT csum FROM notes") == Long.valueOf("302811ae", 16));
	}
	
	@MediumTest
	public void test_addDelTags() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "1");
		deck.addNote(f);
		Note f2 = deck.newNote();
		f2.setitem("Front", "2");
		deck.addNote(f2);
		// adding for a given id
		deck.getTags().bulkAdd(Arrays.asList(new Long[]{f.getId()}), "foo");
		f.load();
		f2.load();
		assertTrue(f.getTags().contains("foo"));
		assertTrue(!f2.getTags().contains("foo"));
		// should be canonified
		deck.getTags().bulkAdd(Arrays.asList(new Long[]{f.getId()}), "foo aaa");
		f.load();
		assertTrue(f.getTags().get(0).equals("aaa"));
		assertTrue(f.getTags().size() == 2);
	}

	@MediumTest
	public void test_timestamps() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertTrue(deck.getModels().getModels().size() == 2);
		for (int i = 0; i < 100; i++) {
			Models.addBasicModel(deck);
		}
		assertTrue(deck.getModels().getModels().size() == 102);
	}

	// test_furigana
}
