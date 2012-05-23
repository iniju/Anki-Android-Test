package com.ichi2.libanki.test;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;

public class CardTestCase extends InstrumentationTestCase {
	
	// previewCards

	@MediumTest
	public void test_delete() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "2");
		deck.addNote(f);
		long cid = f.cards().get(0).getId();
		deck.reset();
		deck.getSched().answerCard(deck.getSched().getCard(), 2);
		assertTrue(deck.getDb().queryScalar("SELECT count() FROM revlog") == 1);
		deck.remCards(new long[]{cid});
		assertTrue(deck.cardCount() == 0);
		assertTrue(deck.noteCount() == 0);
		assertTrue(deck.getDb().queryScalar("SELECT count() FROM notes") == 0);
		assertTrue(deck.getDb().queryScalar("SELECT count() FROM cards") == 0);
		assertTrue(deck.getDb().queryScalar("SELECT count() FROM revlog") == 0);
		assertTrue(deck.getDb().queryScalar("SELECT count() FROM graves") == 2);
	}
	
	@MediumTest
	public void test_misc() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "2");
		deck.addNote(f);
		Card c = f.cards().get(0);
		try {
			long id = deck.getModels().current().getLong("id");
			assertTrue(c.template().getInt("ord") == 0);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	// genrem
	// gendeck

}
