package com.ichi2.libanki.test;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Utils;

public class CardTestCase extends InstrumentationTestCase {
	
	public CardTestCase(String name) {
		setName(name);
	}
	
	@MediumTest
	public void test_previewCards() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "2");
		// non-empty and active
		List<Card> cards = deck.previewCards(f, 0);
		assertTrue(cards.size() == 1);
		assertTrue(cards.get(0).getOrd() == 0);
		// all templates
		cards = deck.previewCards(f, 2);
		assertTrue(cards.size() == 1);
		// add the note, and test existing preview
		deck.addNote(f);
		cards = deck.previewCards(f, 1);
		assertTrue(cards.size() == 1);
		assertTrue(cards.get(0).getOrd() == 0);
		// make sure we haven't accidentally added cards to the db
		assertTrue(deck.cardCount() == 1);
	}
		
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
	
	@MediumTest
	public void test_genrem() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = d.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "");
		d.addNote(f);
		assertTrue(f.cards().size() == 1);
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		// adding a new template should automatically create cards
		JSONObject t = mm.newTemplate("rev");
		try {
			t.put("qfmt", "{{Front}}");
			t.put("afmt", "");
			mm.addTemplate(m, t);
			mm.save(m, true);
			assertTrue(f.cards().size() == 2);
			// if the template is changed to remove cards, they'll be removed
			t.put("qfmt", "{{Back}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.save(m, true);
		d.remCards(Utils.toPrimitive(d.emptyCids()));
		assertTrue(f.cards().size() == 1);
		// if we add to the note, a card should be automatically generated
		f.load();
		f.setitem("Back", "1");
		f.flush();
		assertTrue(f.cards().size() == 2);
		// deletion calls a hook to let the user abort the delete. let's abort it:
		//AnkiDroidApp.getHooks().addHook("remEmptyCards", new AbortHook());
	}
	
	//class AbortHook extends Hook {
	//	@Override
    //    public Object runFilter(Object arg, Object... args) {
    //        return Boolean.FALSE;
    //    }
	//}
	
	@MediumTest
	public void test_gendeck() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject cloze = d.getModels().byName("Cloze");
		d.getModels().setCurrent(cloze);
		Note f = d.newNote();
		f.setitem("Text", "{{c1::one}}");
		d.addNote(f);
		assertTrue(d.cardCount() == 1);
		assertTrue(f.cards().get(0).getDid() == 1);
		// set the model to a new default deck
		long newId = d.getDecks().id("new");
		try {
			cloze.put("did", newId);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().save(cloze);
		// a newly generated card should share the first card's deck
		f.setitem("Text", f.getitem("Text") + "{{c2::two}}");
		f.flush();
		assertTrue(f.cards().get(1).getDid() == 1);
		// and the same with multiple cards
		f.setitem("Text", f.getitem("Text") + "{{c3::three}}");
		f.flush();
		assertTrue(f.cards().get(2).getDid() == 1);
		// if one of the cards is in different deck, it should revert to the model default
		Card c = f.cards().get(1);
		c.setDid(newId);
		c.flush();
		f.setitem("Text", f.getitem("Text") + "{{c4::four}}");
		f.flush();
		assertTrue(f.cards().get(3).getDid() == newId);
	}
}
