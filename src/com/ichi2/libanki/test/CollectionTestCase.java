package com.ichi2.libanki.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.sqlite.SQLiteException;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Storage;

public class CollectionTestCase extends InstrumentationTestCase {
    
	public CollectionTestCase(String name) {
		setName(name);
	}

	// create_test - removed to make tests independent

    @MediumTest
    public void test_open() {
        File dstdir = getInstrumentation().getContext().getExternalCacheDir();
        File dfile = new File(dstdir, "test_attachNew.anki2");
        String path = dfile.getAbsolutePath();
        dfile.delete();
        Collection deck1 = Storage.Collection(path);
        String newPath = deck1.getPath();
        deck1.close();
        long mNewMod = deck1.getMod();
        Collection deck2 = Storage.Collection(newPath);
        assertTrue(deck2.getMod() == mNewMod);
        deck2.close();
    }

    // test_openReadOnly - removed as readonly perms not allowed in external storage
    
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

	@MediumTest
	public void test_furigana() {
		Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
		Models mm = deck.getModels();
		JSONObject m = mm.current();
		// filter should work
		try {
			m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{kana:Front}}");
			mm.save(m);
			Note n = deck.newNote();
			n.setitem("Front", "foo[abc]");
			deck.addNote(n);
			Card c = n.cards().get(0);
			assertTrue(c.getQuestion(false).endsWith("abc"));
			// and should avoid sound
			n.setitem("Front", "foo[sound:abc.mp3]");
			n.flush();
			assertTrue(c.getQuestion(true, false).contains("sound:"));
			// it shouldn't throw an error while people are editing
			m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{kana:}}");
			mm.save(m);
			c.getQuestion(true, false);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	// NOT IN LIBANKI
	@MediumTest
    public void test_more_furigana() {
        Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
        Models mm = deck.getModels();
        JSONObject m = mm.current();
        // filter should work
        try {
            m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{furigana:Front}}");
            mm.save(m);
            Note n = deck.newNote();
            n.setitem("Front", "警察[けいさつ]の 威信[いしん]にかけてこの 男[おとこ]を 捕[つか]まえることが　 俺[おれ]の 使命[しめい]");
            deck.addNote(n);
            Card c = n.cards().get(0);
            assertTrue(c.getQuestion(false).endsWith("<ruby><rb>警察</rb><rt>けいさつ</rt></ruby>の<ruby><rb>威信</rb><rt>いしん</rt></ruby>にかけてこの<ruby><rb>男</rb><rt>おとこ</rt></ruby>を<ruby><rb>捕</rb><rt>つか</rt></ruby>まえることが　<ruby><rb>俺</rb><rt>おれ</rt></ruby>の<ruby><rb>使命</rb><rt>しめい</rt></ruby>"));
            n.setitem("Front", "<font color=\"red\">日本[にほん]</font>");
            n.flush();
            c = n.cards().get(0);
            assertTrue(c.getQuestion(false).endsWith("<font color=\"red\"><ruby><rb>日本</rb><rt>にほん</rt></ruby></font>"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
	
	// NOT IN LIBANKI
    @MediumTest
    public void test_hint() {
        Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
        Models mm = deck.getModels();
        JSONObject m = mm.current();
        try {
            // add a new field to store the hint
            JSONObject field = mm.newField(m.toString());
            field.put("name", "Hint");
            mm.addField(m, field);
            // add hint to question template
            m.getJSONArray("tmpls").getJSONObject(0).put("qfmt", "{{Front}}{{hint:Hint}}");
            mm.save(m);
            // create a new note with hint populated
            Note f = deck.newNote();
            f.setitem("Front", "foo");
            f.setitem("Hint", "myhint");
            deck.addNote(f);
            Card c = f.cards().get(0);
            int id = "myhint".hashCode();
            // hint should appear in question
            assertTrue(c.getQuestion(false).endsWith(String.format(Locale.US, "foo<a href=\"#\" " +
"onclick=\"this.style.display='none';document.getElementById('hint%d').style.display='block';return false;\">" +
"Show Hint</a><div id=\"hint%d\" style=\"display: none\">myhint</div>", id, id)));
            // if hint is empty, then the link shouldn't appear in question
            f.setitem("Hint", "");
            f.flush();
            c = f.cards().get(0);
            assertTrue(c.getQuestion(false).endsWith(String.format(Locale.US, "foo")));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
