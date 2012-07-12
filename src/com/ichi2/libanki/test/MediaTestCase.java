package com.ichi2.libanki.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;

public class MediaTestCase extends InstrumentationTestCase {
	
	public MediaTestCase(String name) {
		setName(name);
	}

	@MediumTest
	public void test_add() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		File mediaFile = null;
		try {
			File dir = getInstrumentation().getTargetContext().getCacheDir();
			mediaFile = new File(dir, "foo.jpg");
			String path = (mediaFile).getAbsolutePath();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
			bos.write(new String("hello").getBytes());
			bos.close();
			// new file, should preserve name
			assertTrue(d.getMedia().addFile(path).equals("foo.jpg"));
			// adding the same file again should not create a duplicate
			assertTrue(d.getMedia().addFile(path).equals("foo.jpg"));
			// but if it has a different md5, it should
			bos = new BufferedOutputStream(new FileOutputStream(path));
			bos.write(new String("world").getBytes());
			bos.close();
			assertTrue(d.getMedia().addFile(path).equals("foo (1).jpg"));
			// Just a bit more testing on unique filename generation -- Kostas
			bos = new BufferedOutputStream(new FileOutputStream(path));
			bos.write(new String("foo").getBytes());
			bos.close();
			assertTrue(d.getMedia().addFile(path).equals("foo (2).jpg"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (mediaFile != null && mediaFile.exists()) {
				mediaFile.delete();
			}
		}
	}
	
	@MediumTest
	public void test_strings() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertTrue(d.getMedia().filesInStr("aoeu").isEmpty());
		assertTrue(d.getMedia().filesInStr("aoeu<img src='foo.jpg'>ao").equals(Arrays.asList(new String[]{"foo.jpg"})));
		assertTrue(d.getMedia().filesInStr("aoeu<img src=foo bar.jpg>ao").equals(Arrays.asList(new String[]{"foo bar.jpg"})));
		assertTrue(d.getMedia().filesInStr("aoeu<img src=\"foo.jpg\">ao").equals(Arrays.asList(new String[]{"foo.jpg"})));
		assertTrue(d.getMedia().filesInStr("aoeu<img src=\"foo.jpg\"><img class=yo src=fo>ao").equals(Arrays.asList(new String[]{"foo.jpg", "fo"})));
		assertTrue(d.getMedia().filesInStr("aou[sound:foo.mp3]aou").equals(Arrays.asList(new String[]{"foo.mp3"})));
		assertTrue(d.getMedia().strip("aoeu").equals("aoeu"));
		assertTrue(d.getMedia().strip("aoeu[sound:foo.mp3]aoeu").equals("aoeuaoeu"));
		assertTrue(d.getMedia().strip("a<img src=yo>oeu").equals("aoeu"));
		assertTrue(d.getMedia().escapeImages("aoeu").equals("aoeu"));
		assertTrue(d.getMedia().escapeImages("<img src='http://foo.com'>").equals("<img src='http://foo.com'>"));
		assertTrue(d.getMedia().escapeImages("<img src=\"foo bar.jpg\">").equals("<img src=\"foo%20bar.jpg\">"));
	}

	@MediumTest
	public void test_deckIntegration() {
		Context ctx = getInstrumentation().getContext();
		Collection d = Shared.getEmptyDeck(ctx);
		// create a media dir
		d.getMedia().getDir();
		// put a file into it
		File file;
		try {
			file = Shared.copyFileFromAssets(ctx, "fake.png", getInstrumentation().getTargetContext().getCacheDir());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		d.getMedia().addFile(file.getAbsolutePath());
		// add a note which references it
		Note f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "<img src='fake.png'>");
		d.addNote(f);
		// and one which references a non-existent file
		f = d.newNote();
		f.setitem("Front", "one");
		f.setitem("Back", "<img src='fake2.png'>");
		d.addNote(f);
		// and add another file which isn't used
		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(new File(d.getMedia().getDir(), "foo.jpg")));
			bos.write(new String("test").getBytes());
			bos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<List<String>> ret = d.getMedia().check();
		assertTrue(ret.get(0).equals(Arrays.asList(new String[]{"fake2.png"})));
		assertTrue(ret.get(1).equals(Arrays.asList(new String[]{"foo.jpg"})));
	}

	@MediumTest
	public void test_changes() {
		Collection d = Shared.getEmptyDeck(getInstrumentation().getContext());
		assertTrue(d.getMedia()._changed() != 0);
		assertTrue(d.getMedia().getMediaDb().queryColumn(String.class, "select fname from log where type = 0", 0).isEmpty());
		assertTrue(d.getMedia().removed().isEmpty());
		// add a file
		File file;
		BufferedOutputStream bos;
		try {
			file = new File(getInstrumentation().getTargetContext().getCacheDir(), "foo.jpg");
			bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(new String("hello").getBytes());
			bos.close();
	        Thread.sleep(1000);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
		    throw new RuntimeException(e);
		} catch (InterruptedException e) {
		    throw new RuntimeException(e);
        }
		String path = d.getMedia().addFile(file.getAbsolutePath());
		// should have been logged
		d.getMedia().findChanges();
		assertTrue(!d.getMedia().getMediaDb().queryColumn(String.class, "select fname from log where type = 0", 0).isEmpty());
		assertTrue(d.getMedia().removed().isEmpty());
		// if we modify it, the cache won't notice
		try {
            Thread.sleep(1000);
			bos = new BufferedOutputStream(new FileOutputStream(new File(d.getMedia().getDir(), path)));
			bos.write(new String("world").getBytes());
			bos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
		}
		assertTrue(d.getMedia().getMediaDb().queryColumn(String.class, "select fname from log where type = 0", 0).size() == 1);
		assertTrue(d.getMedia().removed().isEmpty());
		// but if we add another file, it will
		try {
            Thread.sleep(1000);
			bos = new BufferedOutputStream(new FileOutputStream(new File(d.getMedia().getDir(), path + "2")));
			bos.write(new String("yo").getBytes());
			bos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
		}
		d.getMedia().findChanges();
		assertTrue(d.getMedia().getMediaDb().queryColumn(String.class, "select fname from log where type = 0", 0).size() == 2);
		assertTrue(d.getMedia().removed().isEmpty());
		// deletions should get noticed too
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
		new File(d.getMedia().getDir(), path + "2").delete();
		d.getMedia().findChanges();
		assertTrue(d.getMedia().getMediaDb().queryColumn(String.class, "select fname from log where type = 0", 0).size() == 1);
		assertTrue(d.getMedia().removed().size() == 1);
	}
}
		
