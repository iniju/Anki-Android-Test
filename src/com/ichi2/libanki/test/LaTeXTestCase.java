package com.ichi2.libanki.test;

import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;
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
		MoreAsserts.assertNotEqual(f.cards().get(0).getQuestion(false), oldcard.getQuestion(false));
		// another note with the same media should reuse
		oldcard = f.cards().get(0);
		f = d.newNote();
		f.setitem("Front", "[latex]world[/latex]");
		d.addNote(f);
		assertEquals(f.cards().get(0).getQuestion(false), oldcard.getQuestion(false));
	}

    @MediumTest
    public void test_more_latex() {
        // Test cases from issue 1368
        Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
        Note f = d.newNote();
        f.setitem("Front", "[$]A[/$]");
        d.addNote(f);
        Card c = f.cards().get(0);
        d.addNote(f);
        assertTrue(c.getQuestion(false).contains("<img src=\"latex-019e9892786e493964e145e7c5cf7b700314e53b.png\">"));
        f.setitem("Front", "[$$]\\frac{A_1 e^{i\\varphi_1}}{A_2 e^{i\\varphi_2}}[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-02a0f03e6aeaef7135939dfd4f5d68dd8b34e10e.png\">"));
        f.setitem("Front", "[$$]\\left|z\\right|^2[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-0319612c4826fbf52f897651920a083b0355aa31.png\">"));
        f.setitem("Front", "[$$]E_{n} = E_{total} \\frac{R_{n}}{R_{total}}[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-0fcb517130963a0d5b9a1cd2e07e6b4b4f8fccc6.png\">"));
        f.setitem("Front", "[$]\\overrightarrow{v_{B/R}}=\\overrightarrow{v_{A/R}}+\\overrightarrow{BA}\\wedge\\overrightarrow{\\omega_{S/R}}[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-0d138c544a791923a82e650202b5fe7cea0d4c4a.png\">"));
        f.setitem("Front", "[$]J_\\Delta=\\int_ Sigma r^2 \\mathrm{d}m[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-a052abf4e2cc0ae8854d6d70d049129fd3e8fc17.png\">"));
        f.setitem("Front", "[$]\\vec P=\\displaystyle\\sum_i m_i\\overrightarrow{AM_i}\\wedge\\vec v_i[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-00e49aba3c3bc88436b15dea49755e7af78543ba.png\">"));
        f.setitem("Front", "[$]\\vec \\delta_A=\\vec \\delta^*+\\overrightarrow{AG}\\wedge\\overrightarrow{D}[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-07f90921cb6cc878c8f03ef997abcfb88087d73b.png\">"));
        f.setitem("Front", "[$]\\Delta V+\\dfrac{\\rho}{\\varepsilon_0}=0[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-615941879234b5de48c4fcd598077b236558ea7b.png\">"));
        f.setitem("Front", "[$$]K[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-040d4edac6c20e215eff449c881ded494391c6b9.png\">"));
        f.setitem("Front", "[$$]cosh\\left(x\\right)[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-65d3015ff584f459608a8c81d4dd5c279ffe35b7.png\">"));
        f.setitem("Front", "[$$]\\mathbf{x}'\\left(t\\right)=A\\mathbf{x}\\left(t\\right)+\\mathbf{b}[/$$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-7e45937a00428112d1009e2b02f529af19bcb9cf.png\">"));
        f.setitem("Front", "<div><div><div>[$]a_{11}x_1+a_{12}x_2+\\dotsb+a_{1n}x_n =0\\\\a_{21}x_1+a_{22}x_2+\\dotsb+a_{2n}x_n =0\\\\</div><div>\\vdots\\\\ a_{m1}x_1+a_{m2}x_2+\\dotsb+a_{mn}x_n=0 [/$]</div></div></div>");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-87db9dcf2e63cc6d2dd071a0fd91b4be747b5255.png\">"));
        f.setitem("Front", "[$]\\mathfrak{p}'[/$]");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-06c78978b35ec6a9e7fd3846728315cff15cb85e.png\">"));
        f.setitem("Front", "[$]2+2=4&lt;div&gt;[/$]&lt;/div&gt;");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-bbacdba8bec77ee5e30b278fc10f0d9255bb5ab0.png\">"));
        f.setitem("Front", "<div>[latex]Soit $a\\in \\mathbb{Z}$. L'ordre de $\\dot a$ est :\\\\</div><div>$\\dfrac{n}{n\\wedge a}$</div><div>&nbsp;[/latex]</div>");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-db70eae2ca4c7723e05f8b2b708b2d6117eec6a0.png\">"));
        f.setitem("Front", "<div>[latex]</div><div>\\begin{center}</div><div>Multiply it by the metric:</div><div>\\end{center}</div><div>$$\\Gamma^\\alpha_{\\mu \\kappa}=g_{\\mu\\nu}\\Gamma^{\\alpha \\nu}_\\kappa$$</div><div>[/latex]</div>");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-0699f55d08e4b716314618e678f8b90a266ff753.png\">"));
        f.setitem("Front", "<div><div>[latex]\\begin{enumerate}</div><div>\\item A system is completely described by a wave function $\\Psi$, which represents an observer's knowledge of the system.</div><div><br /></div><div>\\item The description of nature is probabilistic. The probability of an event is the mag squared of the wave function related to it.</div><div><br /></div><div>\\item Heisenberg's Uncertainty Principle says it's impossible to know the values of all the properties of the system at the same time; properties not known with precision are described by probabilities.</div><div><br /></div><div>\\item Complementarity Principle: matter exhibits a wave-particle duality. An experiment can show the particle-like properties of matter or the wave-like properties, but not both at the same time.</div><div><br /></div><div>\\item Measuring devices are essentially classical devices, and they measure classical properties such as position and momentum.</div><div><br /></div><div>\\item The correspondence principle of Bohr and Heisenberg: the quantum mechanical description of large systems should closely approximate the classical description.</div><div>\\end{enumerate}</div><div>[/latex]</div></div>");
        f.flush();
        assertTrue(c.getQuestion(true, false).contains("<img src=\"latex-d3f46f2e4b5cabbef582a6ccbeaf18075bf2565a.png\">"));
    }
}
