package unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.stankay.ewsgmail.GmailClient;

public class GmailClientTest {

	@Test
	public void generateLabelListNull() {
		List<String> output = GmailClient.generateLabelList(null);
		assertEquals(2, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
	}

	@Test
	public void generateLabelListEmpty() {
		List<String> output = GmailClient.generateLabelList("");
		assertEquals(2, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
	}

	@Test
	public void generateLabelList1Item() {
		List<String> output = GmailClient.generateLabelList(" Label_1 ");
		assertEquals(3, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
		assertTrue(output.contains("Label_1"));
	}

	@Test
	public void generateLabelList2Items() {
		List<String> output = GmailClient.generateLabelList(" Label_1 , Label_2 ");
		assertEquals(4, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
		assertTrue(output.contains("Label_1"));
		assertTrue(output.contains("Label_2"));
	}

	@Test
	public void generateLabelListComma() {
		List<String> output = GmailClient.generateLabelList(",");
		assertEquals(2, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
	}

	@Test
	public void generateLabelListComma2() {
		List<String> output = GmailClient.generateLabelList(" ,Label_1, ");
		assertEquals(3, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
		assertTrue(output.contains("Label_1"));
	}

	@Test
	public void generateLabelListInvalidChars() {
		List<String> output = GmailClient.generateLabelList("Label#1");
		assertEquals(2, output.size());
		assertTrue(output.contains("INBOX"));
		assertTrue(output.contains("UNREAD"));
	}

}
