package com.ichi2.libanki.test;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;

public class LaTeXTestCase extends InstrumentationTestCase {
	public LaTeXTestCase(String name) {
		setName(name);
	}
	@MediumTest
	public void test_latex() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		// skipping the image generation and just checking the links in this test
		Note f = d.newNote();
		f.setitem("Front", "[latex]hello[/latex]");
		d.addNote(f);
		assertTrue(f.cards().get(0).getQuestion(false).contains(".png"));
		// adding a different note should create different link
		Card oldcard = f.cards().get(0);
		f = d.newNote();
		f.setitem("Front", "[latex]world[/latex]");
		d.addNote(f);
		assertTrue(f.cards().get(0).getQuestion(false).contains(".png"));
		assertFalse(f.cards().get(0).getQuestion(false).equals(oldcard.getQuestion(false)));
		// another note with the same media should reuse
		oldcard = f.cards().get(0);
		f = d.newNote();
		f.setitem("Front", "[latex]world[/latex]");
		d.addNote(f);
		assertTrue(f.cards().get(0).getQuestion(false).equals(oldcard.getQuestion(false)));
	}
}
