package com.ichi2.libanki.test;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;

public class FindTestCase extends InstrumentationTestCase {
	
	@MediumTest
	public void test_findCards() {
		try {
			Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
			Note f = deck.newNote();
			f.setitem("Front", "dog");
			f.setitem("Back", "cat");
			f.addTag("monkey");
			long f1id = f.getId();
			deck.addNote(f);
			long firstCardId = f.cards().get(0).getId();
			f = deck.newNote();
			f.setitem("Front", "goats are fun");
			f.setitem("Back", "sheep");
			f.addTag("sheep goat horse");
			deck.addNote(f);
			long f2id = f.getId();
			f = deck.newNote();
			f.setitem("Front", "cat");
			f.setitem("Back", "sheep");
			deck.addNote(f);
			Card catCard = f.cards().get(0);
			JSONObject m = deck.getModels().current();
			Models mm = deck.getModels();
			JSONObject t = mm.newTemplate("Reverse");
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
			mm.addTemplate(m,  t);
			mm.save(m);
			f = deck.newNote();
			f.setitem("Front", "template test");
			f.setitem("Back", "foo bar");
			deck.addNote(f);
			ArrayList<Card> cards = f.cards();
			ArrayList<Long> latestCardIds = new ArrayList<Long>();
			for (Card c : cards) {
				latestCardIds.add(c.getId());
			}
			// tag searches
			assertTrue(deck.findCards("tag:donkey").size() == 0);
			assertTrue(deck.findCards("tag:sheep").size() == 1);
			assertTrue(deck.findCards("tag:sheep tag:goat").size() == 1);
			assertTrue(deck.findCards("tag:monkey").size() == 1);
			assertTrue(deck.findCards("tag:sheep -tag:monkey").size() == 1);
			assertTrue(deck.findCards("-tag:sheep").size() == 4);
			// bulkadd --> not yet implemented
			// TODO: add bulkadd/bulkrem tests

			// text searches
			assertTrue(deck.findCards("cat").size() == 2);
			assertTrue(deck.findCards("cat -dog").size() == 1);
			assertTrue(deck.findCards("are goats").size() == 1);
			assertTrue(deck.findCards("'are goats'").size() == 0);
			assertTrue(deck.findCards("'goats are'").size() == 1);
			// card states
			Card c = f.cards().get(0);
			c.setType(2);
			assertTrue(deck.findCards("is:review").size() == 0);
			c.flush();
			assertTrue(deck.findCards("is:review").get(0) == c.getId());
			assertTrue(deck.findCards("is:due").size() == 0);
			c.setDue(0);
			c.setQueue(2);
			c.flush();
			assertTrue(deck.findCards("is:due").get(0) == c.getId());
			assertTrue(deck.findCards("-is:due").size() == 4);
			c.setQueue(-1);
			// ensure this card gets a later mod time
			c.flush();
			assertTrue(deck.findCards("is:suspended").get(0) == c.getId());
			// nids
			assertTrue(deck.findCards("nid:54321").size() == 0);
			assertTrue(deck.findCards("nid:" + f.getId()).size() == 2);
			assertTrue(deck.findCards("nid:" + f1id + "," + f2id).size() == 2);
			// templates
			assertTrue(deck.findCards("card:foo").size() == 0);
			assertTrue(deck.findCards("\'card:card 1\'").size() == 4);
			assertTrue(deck.findCards("card:reverse").size() == 1);
			assertTrue(deck.findCards("card:1").size() == 4);
			assertTrue(deck.findCards("card:2").size() == 1);
			//fields
			assertTrue(deck.findCards("front:dog").size() == 1);
			assertTrue(deck.findCards("-front:dog").size() == 4);
			assertTrue(deck.findCards("front:sheep").size() == 0);
			assertTrue(deck.findCards("back:sheep").size() == 2);
			assertTrue(deck.findCards("-back:sheep").size() == 3);
			assertTrue(deck.findCards("front:").size() == 5);
			// ordering
			deck.getConf().put("sortType", "noteCrt");
			ArrayList<Long> c2 = deck.findCards("front:");
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			c2 = deck.findCards("");
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			deck.getConf().put("sortType", "noteFld");
			c2 = deck.findCards("");
			assertTrue(c2.get(0) == catCard.getId());
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			deck.getConf().put("sortType", "noteMod");
			c2 = deck.findCards("");
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			assertTrue(c2.get(0) == firstCardId);
			deck.getConf().put("sortBackwards", true);
			assertTrue(latestCardIds.contains(c2.get(0)));
			// model
			assertTrue(deck.findCards("note:basic").size() == 5);
			assertTrue(deck.findCards("-note:basic").size() == 0);
			assertTrue(deck.findCards("-note:foo").size() == 5);
			// deck
			assertTrue(deck.findCards("deck:default").size() == 5);
			assertTrue(deck.findCards("-deck:default").size() == 0);
			assertTrue(deck.findCards("-note:foo").size() == 5);
			assertTrue(deck.findCards("deck:def*").size() == 5);
			assertTrue(deck.findCards("deck:*EFAULT").size() == 5);
			assertTrue(deck.findCards("deck:*cefault").size() == 0);
			// full search
			f = deck.newNote();
			f.setitem("Front", "hello<b>world</b>");
			f.setitem("Back", "abc");
			deck.addNote(f);
			// as it's the sort field, it matches
			assertTrue(deck.findCards("helloworld").size() == 0);
			assertTrue(deck.findCards("helloworld", true).size() == 2);
			// if we put it on the back, it won't
			String tmpf = f.getitem("Front");
			String tmpb = f.getitem("Back");
			f.setitem("Front", tmpf);
			f.setitem("Back", tmpb);
			assertTrue(deck.findCards("helloworld").size() == 0);
			assertTrue(deck.findCards("helloworld", true).size() == 2);
			assertTrue(deck.findCards("front:helloworld").size() == 0);
			assertTrue(deck.findCards("front:helloworld", true).size() == 2);
			// searching for an invalid special tag should not error
			assertTrue(deck.findCards("is:invalid").size() == 0);
			// should be able to limit to parent deck, no children
			long id = deck.getDb().queryLongScalar("SELECT id FROM cards LIMIT 1");
			deck.getDb().execute("UPDATE cards SET did = " + deck.getDecks().id("Default::Child") + " WHERE id = " + id);
			assertTrue(deck.findCards("deck:default").size() == 7);
			assertTrue(deck.findCards("deck:default::child").size() == 1);
			assertTrue(deck.findCards("deck:default -deck:default::*").size() == 6);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	// findreplace

}
