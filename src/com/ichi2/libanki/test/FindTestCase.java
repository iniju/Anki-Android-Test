/****************************************************************************************
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/
package com.ichi2.libanki.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Pair;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Finder;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;

public class FindTestCase extends InstrumentationTestCase {
	public FindTestCase(String name) {
		setName(name);
	}
	
	@MediumTest
	public void test_parse() {
		Finder f = new Finder(null);
		assertTrue(Arrays.equals(f._tokenize("hello world"), new String[]{"hello", "world"}));
		assertTrue(Arrays.equals(f._tokenize("hello  world"), new String[]{"hello", "world"}));
		assertTrue(Arrays.equals(f._tokenize("one -two"), new String[]{"one", "-", "two"}));
		assertTrue(Arrays.equals(f._tokenize("one --two"), new String[]{"one", "-", "two"}));
		assertTrue(Arrays.equals(f._tokenize("one - two"), new String[]{"one", "-", "two"}));
		assertTrue(Arrays.equals(f._tokenize("one or -two"), new String[]{"one", "or", "-", "two"}));
		assertTrue(Arrays.equals(f._tokenize("'hello \"world\"'"), new String[]{"hello \"world\""}));
		assertTrue(Arrays.equals(f._tokenize("\"hello world\""), new String[]{"hello world"}));
		assertTrue(Arrays.equals(f._tokenize("one (two or ( three or four))"),
				new String[]{"one", "(", "two", "or", "(", "three", "or", "four", ")", ")"}));
		assertTrue(Arrays.equals(f._tokenize("embedded'string"), new String[]{"embedded'string"}));
		assertTrue(Arrays.equals(f._tokenize("deck:'two words'"), new String[]{"deck:two words"}));
	}
	
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
			f.setitem("Front", "test");
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
			assertTrue(deck.findCards("tag:sheep tag:monkey").size() == 0);
			assertTrue(deck.findCards("tag:monkey").size() == 1);
			assertTrue(deck.findCards("tag:sheep -tag:monkey").size() == 1);
			assertTrue(deck.findCards("-tag:sheep").size() == 4);
			deck.getTags().bulkAdd(deck.getDb().queryColumn(Long.class, "select id from notes", 0), "foo bar");
			assertTrue(deck.findCards("tag:bar").size() == 5);
			assertTrue(deck.findCards("tag:foo").size() == 5);
			deck.getTags().bulkRem(deck.getDb().queryColumn(Long.class, "select id from notes", 0), "foo");
			assertTrue(deck.findCards("tag:foo").size() == 0);
			assertTrue(deck.findCards("tag:bar").size() == 5);
			// text searches
			assertTrue(deck.findCards("cat").size() == 2);
			assertTrue(deck.findCards("cat -dog").size() == 1);
			assertTrue(deck.findCards("are goats").size() == 1);
			assertTrue(deck.findCards("\"are goats\"").size() == 0);
			assertTrue(deck.findCards("\"goats are\"").size() == 1);
			// card states
			Card c = f.cards().get(0);
			c.setType(2);
			assertTrue(deck.findCards("is:review").size() == 0);
			c.flush();
			assertTrue(Arrays.equals(deck.findCards("is:review").toArray(new Long[]{}), new Long[]{c.getId()}));
			assertTrue(deck.findCards("is:due").size() == 0);
			c.setDue(0);
			c.setQueue(2);
			c.flush();
			assertTrue(Arrays.equals(deck.findCards("is:due").toArray(new Long[]{}), new Long[]{c.getId()}));
			assertTrue(deck.findCards("-is:due").size() == 4);
			c.setQueue(-1);
			// ensure this card gets a later mod time
			c.flush();
			deck.getDb().execute("update cards set mod = mod + 1 where id = " + c.getId());
			assertTrue(Arrays.equals(deck.findCards("is:suspended").toArray(new Long[]{}), new Long[]{c.getId()}));
			// nids
			assertTrue(deck.findCards("nid:54321").size() == 0);
			assertTrue(deck.findCards("nid:" + f.getId()).size() == 2);
			assertTrue(deck.findCards(String.format(Locale.US, "nid:%d,%d", f1id, f2id)).size() == 2);
			// templates
			assertTrue(deck.findCards("card:foo").size() == 0);
			assertTrue(deck.findCards("'card:card 1'").size() == 4);
			assertTrue(deck.findCards("card:reverse").size() == 1);
			assertTrue(deck.findCards("card:1").size() == 4);
			assertTrue(deck.findCards("card:2").size() == 1);
			//fields
			assertTrue(deck.findCards("front:dog").size() == 1);
			assertTrue(deck.findCards("-front:dog").size() == 4);
			assertTrue(deck.findCards("front:sheep").size() == 0);
			assertTrue(deck.findCards("back:sheep").size() == 2);
			assertTrue(deck.findCards("-back:sheep").size() == 3);
			assertTrue(deck.findCards("front:do").size() == 0);
			assertTrue(deck.findCards("front:*").size() == 5);
			// ordering
			deck.getConf().put("sortType", "noteCrt");
			List<Long> c2 = deck.findCards("front:*", true);
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			c2 = deck.findCards("", true);
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			deck.getConf().put("sortType", "noteFld");
			c2 = deck.findCards("", true);
			assertTrue(c2.get(0) == catCard.getId());
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			deck.getConf().put("sortType", "cardMod");
			c2 = deck.findCards("", true);
			assertTrue(latestCardIds.contains(c2.get(c2.size() - 1)));
			assertTrue(c2.get(0) == firstCardId);
			deck.getConf().put("sortBackwards", true);
			c2 = deck.findCards("", true);
			assertTrue(latestCardIds.contains(c2.get(0)));
			// model
			assertTrue(deck.findCards("note:basic").size() == 5);
			assertTrue(deck.findCards("-note:basic").size() == 0);
			assertTrue(deck.findCards("-note:foo").size() == 5);
			// deck
			assertTrue(deck.findCards("deck:default").size() == 5);
			assertTrue(deck.findCards("-deck:default").size() == 0);
			assertTrue(deck.findCards("-deck:foo").size() == 5);
			assertTrue(deck.findCards("deck:def*").size() == 5);
			assertTrue(deck.findCards("deck:*EFAULT").size() == 5);
			assertTrue(deck.findCards("deck:*cefault").size() == 0);
			// full search
			f = deck.newNote();
			f.setitem("Front", "hello<b>world</b>");
			f.setitem("Back", "abc");
			deck.addNote(f);
			// as it's the sort field, it matches
			assertTrue(deck.findCards("helloworld").size() == 2);
			//assertTrue(deck.findCards("helloworld", full=true).size() == 2);
			// if we put it on the back, it won't
			String tmpf = f.getitem("Front");
			String tmpb = f.getitem("Back");
			f.setitem("Front", tmpb);
			f.setitem("Back", tmpf);
			f.flush();
			assertTrue(deck.findCards("helloworld").size() == 0);
			//assertTrue(deck.findCards("helloworld", full=true).size() == 2);
			//assertTrue(deck.findCards("back:helloworld", full=true).size() == 2);
			// searching for an invalid special tag should not error
			assertTrue(deck.findCards("is:invalid").size() == 0);
			// should be able to limit to parent deck, no children
			long id = deck.getDb().queryLongScalar("SELECT id FROM cards LIMIT 1");
			deck.getDb().execute("UPDATE cards SET did = " + deck.getDecks().id("Default::Child") + " WHERE id = " + id);
			assertTrue(deck.findCards("deck:default").size() == 7);
			assertTrue(deck.findCards("deck:default::child").size() == 1);
			assertTrue(deck.findCards("deck:default -deck:default::*").size() == 6);
			// properties
			id = deck.getDb().queryLongScalar("SELECT id FROM cards LIMIT 1");
			deck.getDb().execute(
					"update cards set queue=2, ivl=10, reps=20, due=30, factor=2200 where id = ?",
					new Object[]{id});
			assertTrue(deck.findCards("prop:ivl>5").size() == 1);
			assertTrue(deck.findCards("prop:ivl<5").size() > 1);
			assertTrue(deck.findCards("prop:ivl>=5").size() == 1);
			assertTrue(deck.findCards("prop:ivl=9").size() == 0);
			assertTrue(deck.findCards("prop:ivl=10").size() == 1);
			assertTrue(deck.findCards("prop:ivl!=10").size() > 1);
			assertTrue(deck.findCards("prop:due>0").size() == 1);
			// due dates should work
			deck.getSched().setToday(15);
			assertTrue(deck.findCards("prop:due=14").size() == 0);
			assertTrue(deck.findCards("prop:due=15").size() == 1);
			assertTrue(deck.findCards("prop:due=16").size() == 0);
			// including negatives
			deck.getSched().setToday(32);
			assertTrue(deck.findCards("prop:due=-1").size() == 0);
			assertTrue(deck.findCards("prop:due=-2").size() == 1);
			// ease factors
			assertTrue(deck.findCards("prop:ease=2.3").size() == 0);
			assertTrue(deck.findCards("prop:ease=2.2").size() == 1);
			assertTrue(deck.findCards("prop:ease>2").size() == 1);
			assertTrue(deck.findCards("-prop:ease>2").size() > 1);
			// recently failed
			assertTrue(deck.findCards("rated:1:1").size() == 0);
			assertTrue(deck.findCards("rated:1:2").size() == 0);
			c = deck.getSched().getCard();
			deck.getSched().answerCard(c, 2);
			assertTrue(deck.findCards("rated:1:1").size() == 0);
			assertTrue(deck.findCards("rated:1:2").size() == 1);
			c = deck.getSched().getCard();
			deck.getSched().answerCard(c, 1);
			assertTrue(deck.findCards("rated:1:1").size() == 1);
			assertTrue(deck.findCards("rated:1:2").size() == 1);
			assertTrue(deck.findCards("rated:1").size() == 2);
			assertTrue(deck.findCards("rated:0:2").size() == 0);
			assertTrue(deck.findCards("rated:2:2").size() == 1);
			// empty field
			assertTrue(deck.findCards("front:").size() == 0);
			f = deck.newNote();
			f.setitem("Front", "");
			f.setitem("Back", "abc2");
			assertTrue(deck.addNote(f) == 1);
			assertTrue(deck.findCards("front:").size() == 1);
			// OR searches and nesting
			assertTrue(deck.findCards("tag:monkey or tag:sheep").size() == 2);
			assertTrue(deck.findCards("(tag:monkey OR tag:sheep)").size() == 2);
			assertTrue(deck.findCards("-(tag:monkey OR tag:sheep)").size() == 6);
			assertTrue(deck.findCards("tag:monkey or (tag:sheep sheep)").size() == 2);
			assertTrue(deck.findCards("tag:monkey or (tag:sheep octopus)").size() == 1);
			// invalid grouping shouldn't error
			assertTrue(deck.findCards(")").size() == 0);
			assertTrue(deck.findCards("(()").size() == 0);
			// added
			assertTrue(deck.findCards("added:0").size() == 0);
			deck.getDb().execute("update cards set id = id - 86400*1000 where id = " + id);
			assertTrue(deck.findCards("added:1").size() == deck.cardCount() - 1);
			assertTrue(deck.findCards("added:2").size() == deck.cardCount());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@MediumTest
	public void test_findReplace() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "foo");
		f.setitem("Back", "bar");
		f.addTag("monkey");
		deck.addNote(f);
		Note f2 = deck.newNote();
		f2.setitem("Front", "baz");
		f2.setitem("Back", "foo");
		f2.addTag("monkey");
		deck.addNote(f2);
		List<Long> nids = new ArrayList<Long>(Arrays.asList(new Long[]{f.getId(), f2.getId()}));
		// should do nothing
		assertTrue(deck.findReplace(nids, "abc", "123") == 0);
		// global replace
		assertTrue(deck.findReplace(nids, "foo", "qux") == 2);
		f.load();
		assertTrue(f.getitem("Front").equals("qux"));
		f2.load();
		assertTrue(f2.getitem("Back").equals("qux"));
		// single field replace
		assertTrue(deck.findReplace(nids, "qux", "foo", "Front") == 1);
		f.load();
		assertTrue(f.getitem("Front").equals("foo"));
		f2.load();
		assertTrue(f2.getitem("Back").equals("qux"));
		// regex replace
		assertTrue(deck.findReplace(nids, "B.r", "reg") == 0);
		f.load();
		assertTrue(!f.getitem("Back").equals("reg"));
		assertTrue(deck.findReplace(nids, "B.r", "reg", true) == 1);
		f.load();
		assertTrue(f.getitem("Back").equals("reg"));
	}
	
	@MediumTest
	public void test_findDupes() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "foo");
		f.setitem("Back", "bar");
		deck.addNote(f);
		Note f2 = deck.newNote();
		f2.setitem("Front", "baz");
		f2.setitem("Back", "bar");
		deck.addNote(f2);
		Note f3 = deck.newNote();
		f3.setitem("Front", "quux");
		f3.setitem("Back", "bar");
		deck.addNote(f3);
		Note f4 = deck.newNote();
		f4.setitem("Front", "quuux");
		f4.setitem("Back", "nope");
		deck.addNote(f4);
		List<Pair<String, List<Long>>> r = deck.findDupes("Back");
		assertTrue(r.get(0).first.equals("bar"));
		assertTrue(r.get(0).second.size() == 3);
		// valid search
		r = deck.findDupes("Back", "bar");
		assertTrue(r.get(0).first.equals("bar"));
		assertTrue(r.get(0).second.size() == 3);
		// excludes everything
		r = deck.findDupes("Back", "invalid");
		assertTrue(r.size() == 0);
		// front isn't dupe
		assertTrue(deck.findDupes("Front").size() == 0);
	}
}
