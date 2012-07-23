
package com.ichi2.libanki.test;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Sched;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class DecksTestCase extends InstrumentationTestCase {
    public DecksTestCase(String name) {
        setName(name);
    }


    @MediumTest
    public void test_basic() {
        Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
        // we start with a standard deck
        assertTrue(deck.getDecks().getDecks().size() == 1);
        // it should have an id of 1
        assertTrue(deck.getDecks().name(1).equals("Default"));
        // create a new deck
        long parentId = deck.getDecks().id("new deck");
        assertTrue(parentId != 0);
        assertTrue(deck.getDecks().getDecks().size() == 2);
        // should get the same id
        assertTrue(deck.getDecks().id("new deck") == parentId);
        // we start with the default deck selected
        assertTrue(deck.getDecks().selected() == 1);
        assertTrue(deck.getDecks().active().equals(Arrays.asList(new Long[] { new Long(1) })));
        // we can select a different deck
        deck.getDecks().select(parentId);
        assertTrue(deck.getDecks().selected() == parentId);
        assertTrue(deck.getDecks().active().equals(Arrays.asList(new Long[] { new Long(parentId) })));
        // let's create a child
        long childId = deck.getDecks().id("new deck::child");
        // it should have been added to the active list
        assertTrue(deck.getDecks().selected() == parentId);
        assertTrue(deck.getDecks().active().equals(Arrays.asList(new Long[] { new Long(parentId), new Long(childId) })));
        // we can select the child individually too
        deck.getDecks().select(childId);
        assertTrue(deck.getDecks().selected() == childId);
        assertTrue(deck.getDecks().active().equals(Arrays.asList(new Long[] { new Long(childId) })));
        // parents with a different case should be handled correctly
        deck.getDecks().id("ONE");
        JSONObject m = deck.getModels().current();
        try {
            m.put("did", deck.getDecks().id("one::two"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        deck.getModels().save(m);
        Note n = deck.newNote();
        n.setitem("Front", "abc");
        deck.addNote(n);
        // this will error if child and parent case don't match
        deck.getSched().deckDueList(Sched.DECK_INFORMATION_SIMPLE_COUNTS);
        // NOT IN LIBANKI
        deck.close(false);
        deck.reopen();
        assertTrue(deck.getDb().queryScalar("select ver from col") == Collection.SCHEMA_VERSION);
    }
    
    @MediumTest
    public void test_remove() {
        Collection deck = Shared.getEmptyDeck(getInstrumentation().getContext());
        // create a new deck, and add a note/card to it
        long g1 = deck.getDecks().id("g1");
        Note f = deck.newNote();
        f.setitem("Front", "1");
        try {
            f.model().put("did", g1);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        deck.addNote(f);
        Card c = f.cards().get(0);
        assertTrue(c.getDid() == g1);
        // by default deleting the deck leaves the cards with an invalid did
        assertTrue(deck.cardCount() == 1);
        deck.getDecks().rem(g1);
        assertTrue(deck.cardCount() == 1);
        c.load();
        assertTrue(c.getDid() == g1);
        // but if we try to get it, we get the default
        assertTrue(deck.getDecks().name(c.getDid()).equals("[no deck]"));
        // let's create another deck and explicitly set the card to it
        long g2 = deck.getDecks().id("g2");
        c.setDid(g2);
        c.flush();
        // this time we'll delete the card/note too
        deck.getDecks().rem(g2, true);
        assertTrue(deck.cardCount() == 0);
        assertTrue(deck.noteCount() == 0);
    }
    
    @MediumTest
    public void test_rename() {
        Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
        long id = d.getDecks().id("hello::world");
        // should be able to rename into a completely different branch,
        // creating parents as necessary
        d.getDecks().rename(d.getDecks().get(id), "foo::bar");
        assertTrue(d.getDecks().allNames().contains("foo"));
        assertTrue(d.getDecks().allNames().contains("foo::bar"));
        assertFalse(d.getDecks().allNames().contains("hello::world"));
        // create another deck
        id = d.getDecks().id("tmp");
        // we can't rename it if it conflicts
        assertFalse(d.getDecks().rename(d.getDecks().get(id), "foo"));
        // when renaming, the children should be renamed too
        d.getDecks().id("one::two::three");
        id = d.getDecks().id("one");
        d.getDecks().rename(d.getDecks().get(id), "yo");
        for (String n : new String[] { "yo", "yo::two", "yo::two::three" }) {
            assertTrue(d.getDecks().allNames().contains(n));
        }
    }
    
    // test_renameForDragAndDrop - no such functionality in AnkiDroid
}
