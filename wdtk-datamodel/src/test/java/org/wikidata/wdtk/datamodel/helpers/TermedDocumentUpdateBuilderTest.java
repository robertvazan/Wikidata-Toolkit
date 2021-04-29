/*
 * #%L
 * Wikidata Toolkit Data Model
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wikidata.wdtk.datamodel.helpers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.TermedStatementDocumentUpdate;

public class TermedDocumentUpdateBuilderTest {

	private static final ItemIdValue Q1 = EntityUpdateBuilderTest.Q1;
	private static final PropertyIdValue P1 = EntityUpdateBuilderTest.P1;
	private static final MediaInfoIdValue M1 = EntityUpdateBuilderTest.M1;
	private static final ItemDocument ITEM = EntityUpdateBuilderTest.ITEM;
	private static final PropertyDocument PROPERTY = EntityUpdateBuilderTest.PROPERTY;
	private static final Statement JOHN_HAS_BROWN_HAIR = StatementUpdateBuilderTest.JOHN_HAS_BROWN_HAIR;
	private static final MonolingualTextValue EN = TermUpdateBuilderTest.EN;
	private static final MonolingualTextValue EN2 = TermUpdateBuilderTest.EN2;
	private static final MonolingualTextValue DE = TermUpdateBuilderTest.DE;
	private static final MonolingualTextValue DE2 = TermUpdateBuilderTest.DE2;
	private static final MonolingualTextValue SK = TermUpdateBuilderTest.SK;
	private static final MonolingualTextValue CS = TermUpdateBuilderTest.CS;
	private static final MonolingualTextValue FR = TermUpdateBuilderTest.FR;
	private static final MonolingualTextValue ES = Datamodel.makeMonolingualTextValue("hola", "es");

	@Test
	public void testForEntityId() {
		assertThrows(NullPointerException.class, () -> TermedDocumentUpdateBuilder.forEntityId(null));
		assertThrows(IllegalArgumentException.class, () -> TermedDocumentUpdateBuilder.forEntityId(ItemIdValue.NULL));
		assertThrows(IllegalArgumentException.class, () -> TermedDocumentUpdateBuilder.forEntityId(M1));
		assertThat(TermedDocumentUpdateBuilder.forEntityId(Q1), is(instanceOf(ItemUpdateBuilder.class)));
		assertThat(TermedDocumentUpdateBuilder.forEntityId(P1), is(instanceOf(PropertyUpdateBuilder.class)));
	}

	@Test
	public void testForBaseRevision() {
		assertThrows(NullPointerException.class, () -> TermedDocumentUpdateBuilder.forBaseRevision(null));
		assertThrows(IllegalArgumentException.class,
				() -> TermedDocumentUpdateBuilder.forBaseRevision(Datamodel.makeItemDocument(ItemIdValue.NULL)));
		assertThat(TermedDocumentUpdateBuilder.forBaseRevision(ITEM), is(instanceOf(ItemUpdateBuilder.class)));
		assertThat(TermedDocumentUpdateBuilder.forBaseRevision(PROPERTY), is(instanceOf(PropertyUpdateBuilder.class)));
	}

	@Test
	public void testStatementUpdate() {
		TermedStatementDocumentUpdate update = TermedDocumentUpdateBuilder.forEntityId(Q1)
				.updateStatements(StatementUpdateBuilder.create().addStatement(JOHN_HAS_BROWN_HAIR).build())
				.build();
		assertThat(update.getStatements().getAddedStatements(), containsInAnyOrder(JOHN_HAS_BROWN_HAIR));
	}

	@Test
	public void testLabelUpdate() {
		TermedStatementDocumentUpdate update = TermedDocumentUpdateBuilder.forEntityId(Q1)
				.updateLabels(TermUpdateBuilder.create().removeTerm("en").build())
				.build();
		assertThat(update.getLabels().getRemovedTerms(), containsInAnyOrder("en"));
	}

	@Test
	public void testBlindDescriptionUpdate() {
		assertThrows(NullPointerException.class,
				() -> TermedDocumentUpdateBuilder.forEntityId(Q1).updateDescriptions(null));
		TermedStatementDocumentUpdate update = TermedDocumentUpdateBuilder.forEntityId(Q1)
				.updateDescriptions(TermUpdateBuilder.create().removeTerm("en").build())
				.updateDescriptions(TermUpdateBuilder.create().removeTerm("sk").build())
				.build();
		assertThat(update.getDescriptions().getRemovedTerms(), containsInAnyOrder("en", "sk"));
	}

	@Test
	public void testBaseDescriptionUpdate() {
		TermedStatementDocumentUpdate update = TermedDocumentUpdateBuilder
				.forBaseRevision(ITEM
						.withDescription(EN)
						.withDescription(SK))
				.updateDescriptions(TermUpdateBuilder.create()
						.setTerm(SK) // ignored
						.removeTerm("en") // checked
						.build())
				.build();
		assertThat(update.getDescriptions().getModifiedTerms(), is(anEmptyMap()));
		assertThat(update.getDescriptions().getRemovedTerms(), containsInAnyOrder("en"));
	}

	@Test
	public void testBlindAliasChanges() {
		TermedDocumentUpdateBuilder builder = TermedDocumentUpdateBuilder.forEntityId(Q1);
		assertThrows(NullPointerException.class, () -> builder.setAliases(null, Collections.emptyList()));
		assertThrows(IllegalArgumentException.class, () -> builder.setAliases(" ", Collections.emptyList()));
		assertThrows(NullPointerException.class, () -> builder.setAliases("en", null));
		assertThrows(NullPointerException.class, () -> builder.setAliases("en", Arrays.asList("hello", null)));
		assertThrows(IllegalArgumentException.class, () -> builder.setAliases("en", Arrays.asList("hello", "hello")));
		TermedStatementDocumentUpdate update = builder
				.setAliases("sk", Arrays.asList(SK.getText())) // single value
				.setAliases("en", Arrays.asList(EN.getText(), EN2.getText())) // multiple values
				.setAliases("cs", Collections.emptyList()) // remove aliases
				.setAliases("de", Arrays.asList(DE.getText()))
				.setAliases("de", Arrays.asList(DE2.getText())) // overwrite previous value
				.build();
		assertThat(update.getAliases().keySet(), containsInAnyOrder("de", "en", "cs", "sk"));
		assertEquals(Arrays.asList(SK), update.getAliases().get("sk"));
		assertEquals(Arrays.asList(EN, EN2), update.getAliases().get("en"));
		assertThat(update.getAliases().get("cs"), is(empty()));
		assertEquals(Arrays.asList(DE2), update.getAliases().get("de"));
	}

	@Test
	public void testBaseAliasChanges() {
		TermedDocumentUpdateBuilder builder = TermedDocumentUpdateBuilder.forBaseRevision(ITEM
				.withAliases("en", Arrays.asList(EN))
				.withAliases("de", Arrays.asList(DE))
				.withAliases("cs", Arrays.asList(CS))
				.withAliases("fr", Arrays.asList(FR))
				.withAliases("es", Arrays.asList(ES)));
		TermedStatementDocumentUpdate update = builder
				.setAliases("sk", Arrays.asList(SK.getText())) // add alias
				.setAliases("cs", Collections.emptyList()) // remove alias
				.setAliases("de", Arrays.asList(DE2.getText())) // modify alias
				.setAliases("pl", Collections.emptyList()) // remove non-existent
				.setAliases("es", Arrays.asList(ES.getText())) // same value
				.setAliases("en", Arrays.asList(EN2.getText()))
				.setAliases("en", Arrays.asList(EN.getText())) // revert modification
				.setAliases("fr", Collections.emptyList())
				.setAliases("fr", Arrays.asList(FR.getText())) // revert removal
				.setAliases("el", Arrays.asList("?"))
				.setAliases("el", Collections.emptyList()) // revert addition
				.build();
		assertThat(update.getAliases().keySet(), containsInAnyOrder("de", "cs", "sk"));
		assertEquals(Arrays.asList(DE2), update.getAliases().get("de"));
		assertThat(update.getAliases().get("cs"), is(empty()));
		assertEquals(Arrays.asList(SK), update.getAliases().get("sk"));
	}

	@Test
	public void testMerge() {
		assertThrows(NullPointerException.class, () -> TermedDocumentUpdateBuilder.forEntityId(Q1).apply(null));
		TermedDocumentUpdateBuilder builder = TermedDocumentUpdateBuilder.forEntityId(Q1)
				.updateLabels(TermUpdateBuilder.create().removeTerm("pl").build())
				.updateDescriptions(TermUpdateBuilder.create().removeTerm("fr").build())
				.setAliases("en", Arrays.asList(EN.getText()));
		builder.apply(TermedDocumentUpdateBuilder.forEntityId(Q1)
				.updateLabels(TermUpdateBuilder.create().removeTerm("sk").build())
				.updateDescriptions(TermUpdateBuilder.create().removeTerm("es").build())
				.setAliases("de", Arrays.asList(DE.getText()))
				.build());
		TermedStatementDocumentUpdate update = builder.build();
		assertThat(update.getLabels().getRemovedTerms(), containsInAnyOrder("sk", "pl"));
		assertThat(update.getDescriptions().getRemovedTerms(), containsInAnyOrder("es", "fr"));
		assertThat(update.getAliases().keySet(), containsInAnyOrder("en", "de"));
		assertEquals(Arrays.asList(EN), update.getAliases().get("en"));
		assertEquals(Arrays.asList(DE), update.getAliases().get("de"));
	}

}
