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

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ModelsTestCase extends InstrumentationTestCase {
	public ModelsTestCase(String name) {
		setName(name);
	}
	
	@MediumTest
	public void test_modelDelete() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note f = deck.newNote();
		f.setitem("Front", "1");
		f.setitem("Back", "2");
		deck.addNote(f);
		assertTrue(deck.cardCount() == 1);
		deck.getModels().rem(deck.getModels().current());
		assertTrue(deck.cardCount() == 0);
	}
	
	@MediumTest
	public void test_modelCopy() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = deck.getModels().current();
		JSONObject m2 = deck.getModels().copy(m);
		try {
			assertTrue(m2.getString("name").equals("Basic copy"));
			assertTrue(m2.getInt("id") != m.getInt("id"));
			assertTrue(m2.getJSONArray("flds").length() == 2);
			assertTrue(m.getJSONArray("flds").length() == 2);
			assertTrue(m2.getJSONArray("flds").length() == m.getJSONArray("flds").length());
			assertTrue(m.getJSONArray("tmpls").length() == 1);
			assertTrue(m2.getJSONArray("tmpls").length() == 1);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		assertTrue(deck.getModels().scmhash(m) == deck.getModels().scmhash(m2));
	}
	
	@MediumTest
	public void test_fields() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		Note fct = d.newNote();
		fct.setitem("Front", "1");
		fct.setitem("Back", "2");
		d.addNote(fct);
		JSONObject m = d.getModels().current();
		// make sure renaming a field updates the templates
		try {
			d.getModels().renameField(m, m.getJSONArray("flds").getJSONObject(0), "NewFront");
			assertTrue(m.getJSONArray("tmpls").getJSONObject(0).getString("qfmt").contains("{{NewFront}}"));
			String h = d.getModels().scmhash(m);
			// add a field
			JSONObject f = d.getModels().newField(m.toString());
			f.put("name", "foo");
			d.getModels().addField(m, f);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"1", "2", ""}));
			assertTrue(!d.getModels().scmhash(m).equals(h));
			// rename it
			d.getModels().renameField(m, f, "bar");
			assertTrue(d.getNote(d.getModels().nids(m).get(0)).getitem("bar").equals(""));
			// delete back
			d.getModels().remField(m, m.getJSONArray("flds").getJSONObject(1));
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"1", ""}));
			// move 0 -> 1
			d.getModels().moveField(m, m.getJSONArray("flds").getJSONObject(0), 1);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"", "1"}));
			// move 1 -> 0
			d.getModels().moveField(m, m.getJSONArray("flds").getJSONObject(1), 0);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"1", ""}));
			// add another and put in middle
			f = d.getModels().newField(m.toString());
			f.put("name", "baz");
			d.getModels().addField(m, f);
			fct = d.getNote(d.getModels().nids(m).get(0));
			fct.setitem("baz", "2");
			fct.flush();
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"1", "", "2"}));
			// move 2 -> 1
			d.getModels().moveField(m, m.getJSONArray("flds").getJSONObject(2), 1);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"1", "2", ""}));
			// move 0 -> 2
			d.getModels().moveField(m, m.getJSONArray("flds").getJSONObject(0), 2);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"2", "", "1"}));
			// move 0 -> 1
			d.getModels().moveField(m, m.getJSONArray("flds").getJSONObject(0), 1);
			assertTrue(Arrays.equals(d.getNote(d.getModels().nids(m).get(0)).getFields(), new String[]{"", "2", "1"}));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	@MediumTest
	public void test_templates() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		JSONObject t = mm.newTemplate("Reverse");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
			mm.addTemplate(m, t);
			mm.save();
			Note f = d.newNote();
			f.setitem("Front", "1");
			f.setitem("Back", "2");
			d.addNote(f);
			assertTrue(d.cardCount() == 2);
			Card c = f.cards().get(0);
			Card c2 = f.cards().get(1);
			// first card should have first ord
			assertTrue(c.getOrd() == 0);
			assertTrue(c2.getOrd() == 1);
			// switch templates
			d.getModels().moveTemplate(m, c.template(), 1);
			c.load();
			c2.load();
			assertTrue(c.getOrd() == 1);
			assertTrue(c2.getOrd() == 0);
			// removing a template should delete its cards
			assertTrue(d.getModels().remTemplate(m, m.getJSONArray("tmpls").getJSONObject(0)));
			assertTrue(d.cardCount() == 1);
			// and should have updated the other cards' ordinals
			c = f.cards().get(0);
			assertTrue(c.getOrd() == 0);
			assertTrue(Utils.stripHTML(c.getQuestion(false)).equals("1"));
			// it shouldn't be possible to orphan notes by removing templates
			t = mm.newTemplate(m.toString());
			mm.addTemplate(m, t);
			assertFalse(d.getModels().remTemplate(m, m.getJSONArray("tmpls").getJSONObject(0)));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	@MediumTest
	public void test_text() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = d.getModels().current();
		try {
			m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{text:Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		d.getModels().save();
		Note f = d.newNote();
		f.setitem("Front", "hello<b>world");
		d.addNote(f);
		assertTrue(f.cards().get(0).getQuestion(false).contains("helloworld"));
	}
	
	@MediumTest
	public void test_cloze() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		d.getModels().setCurrent(d.getModels().byName("Cloze"));
		Note f = d.newNote();
		try {
			assertTrue(f.model().getString("name").equals("Cloze"));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		// a cloze model with no clozes is not empty
		f.setitem("Text", "nothing");
		assertTrue(d.addNote(f) != 0);
		// try with one cloze
		f = d.newNote();
		f.setitem("Text", "hello {{c1::world}}");
		assertTrue(d.addNote(f) == 1);
		assertTrue(f.cards().get(0).getQuestion(false).contains("hello <span class=cloze>[...]</span>"));
		assertTrue(f.cards().get(0).getAnswer(false).contains("hello <span class=cloze>world</span>"));
		// and with a comment
		f = d.newNote();
		f.setitem("Text", "hello {{c1::world::typical}}");
		assertTrue(d.addNote(f) == 1);
		assertTrue(f.cards().get(0).getQuestion(false).contains("hello <span class=cloze>[typical]</span>"));
		assertTrue(f.cards().get(0).getAnswer(false).contains("hello <span class=cloze>world</span>"));
		// and with 2 clozes
		f = d.newNote();
		f.setitem("Text", "hello {{c1::world}} {{c2::bar}}");
		assertTrue(d.addNote(f) == 2);
		Card c1 = f.cards().get(0);
		Card c2 = f.cards().get(1);
		assertTrue(c1.getQuestion(false).contains("<span class=cloze>[...]</span> bar"));
		assertTrue(c1.getAnswer(false).contains("<span class=cloze>world</span> bar"));
		assertTrue(c2.getQuestion(false).contains("world <span class=cloze>[...]</span>"));
		assertTrue(c2.getAnswer(false).contains("world <span class=cloze>bar</span>"));
		// if there are multiple answers for a single cloze, they are given in a list
		f = d.newNote();
		f.setitem("Text", "a {{c1::b}} {{c1::c}}");
		assertTrue(d.addNote(f) == 1);
		assertTrue(f.cards().get(0).getAnswer(false).contains("<span class=cloze>b</span> <span class=cloze>c</span>"));
		// if we add another cloze, a card should be generated
		int cnt = d.cardCount();
		f.setitem("Text", "{{c2::hello}} {{c1::foo}}");
		f.flush();
		assertTrue(d.cardCount() == cnt + 1);
		// 0 or negative indices are not supported
		f.setitem("Text", f.getitem("Text") + "{{c0::hello}} {{c-1::foo}}");
		f.flush();
		assertTrue(f.cards().size() == 2);
	}
	
	@MediumTest
	public void test_modelChange() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject basic = deck.getModels().byName("Basic");
		JSONObject cloze = deck.getModels().byName("Cloze");
		// enable second template and add a note
		JSONObject m = deck.getModels().current();
		Models mm = deck.getModels();
		JSONObject t = mm.newTemplate("Reverse");
		try {
			t.put("qfmt", "{{Back}}");
			t.put("afmt", "{{Front}}");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		mm.addTemplate(m, t);
		mm.save(m);
		Note f = deck.newNote();
		f.setitem("Front", "f");
		f.setitem("Back", "b123");
		deck.addNote(f);
		// switch fields
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(0, 1);
		map.put(1, 0);
		deck.getModels().change(basic, new long[]{f.getId()}, basic, map, null);
		f.load();
		assertTrue(f.getitem("Front").equals("b123"));
		assertTrue(f.getitem("Back").equals("f"));
		// switch cards
		Card c0 = f.cards().get(0);
		Card c1 = f.cards().get(1);
		assertTrue(c0.getQuestion(false).contains("b123"));
		assertTrue(c1.getQuestion(false).contains("f"));
		assertTrue(c0.getOrd() == 0);
		assertTrue(c1.getOrd() == 1);
		deck.getModels().change(basic, new long[]{f.getId()}, basic, null, map);
		f.load();
		c0.load();
		c1.load();
		assertTrue(c0.getQuestion(false).contains("f"));
		assertTrue(c1.getQuestion(false).contains("b123"));
		assertTrue(c0.getOrd() == 1);
		assertTrue(c1.getOrd() == 0);
		// .cards returns cards in order
		assertTrue(f.cards().get(0).getId() == c1.getId());
		// delete first card
		map.put(0, null);
		map.put(1, 1);
		deck.getModels().change(basic, new long[]{f.getId()}, basic, null, map);
		f.load();
		assertTrue(c0.load());
		// the card was deleted. We don't throw exception as in anki, but return false
		assertFalse(c1.load());
		// but we have two cards, as one was generated
		assertTrue(f.cards().size() == 2);
		// an unmapped field becomes blank
		assertTrue(f.getitem("Front").equals("b123"));
		assertTrue(f.getitem("Back").equals("f"));
		deck.getModels().change(basic, new long[]{f.getId()}, basic, map, null);
		f.load();
		assertTrue(f.getitem("Front").equals(""));
		assertTrue(f.getitem("Back").equals("f"));
		// another note to try model conversion
		f = deck.newNote();
		f.setitem("Front", "f2");
		f.setitem("Back", "b2");
		deck.addNote(f);
		assertTrue(deck.getModels().useCount(basic) == 2);
		assertTrue(deck.getModels().useCount(cloze) == 0);
		map.put(0, 0);
		map.put(1, 1);
		deck.getModels().change(basic, new long[]{f.getId()}, cloze, map, map);
		f.load();
		assertTrue(f.getitem("Text").equals("f2"));
		assertTrue(f.cards().size() ==2);
		// back the other way, with deletion of second ord
		try {
			deck.getModels().remTemplate(basic, basic.getJSONArray("tmpls").getJSONObject(1));
			assertTrue(deck.getDb().queryScalar("SELECT count() FROM cards WHERE nid = " + f.getId()) == 2);
			deck.getModels().change(cloze, new long[]{f.getId()}, basic, map, map);
			assertTrue(deck.getDb().queryScalar("SELECT count() FROM cards WHERE nid = " + f.getId()) == 1);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@MediumTest
	public void test_availOrds() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		JSONObject m = d.getModels().current();
		Models mm = d.getModels();
		try {
			JSONObject t = m.getJSONArray("tmpls").getJSONObject(0);
			Note f = d.newNote();
			f.setitem("Front", "1");
			// simple templates
			assertTrue(Arrays.equals(mm.availOrds(m, Utils.joinFields(f.getFields())).toArray(new Integer[]{}), new Integer[]{0}));
			t.put("qfmt", "{{Back}}");
			mm.save(m, true);
			assertTrue(mm.availOrds(m, Utils.joinFields(f.getFields())).isEmpty());
			// AND
			t.put("qfmt", "{{#Front}}{{#Back}}{{Front}}{{/Back}}{{/Front}}");
			mm.save(m, true);
			assertTrue(mm.availOrds(m, Utils.joinFields(f.getFields())).isEmpty());
			t.put("qfmt", "{{#Front}}\n{{#Back}}\n{{Front}}\n{{/Back}}\n{{/Front}}");
			mm.save(m, true);
			assertTrue(mm.availOrds(m, Utils.joinFields(f.getFields())).isEmpty());
			// OR
			t.put("qfmt", "{{Front}}\n{{Back}}");
			mm.save(m, true);
			assertTrue(Arrays.equals(mm.availOrds(m, Utils.joinFields(f.getFields())).toArray(new Integer[]{}), new Integer[]{0}));
			t.put("Front", "");
			t.put("Back", "1");
			assertTrue(Arrays.equals(mm.availOrds(m, Utils.joinFields(f.getFields())).toArray(new Integer[]{}), new Integer[]{0}));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
}
