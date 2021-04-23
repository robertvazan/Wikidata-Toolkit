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

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermedStatementDocument;
import org.wikidata.wdtk.datamodel.interfaces.TermedStatementDocumentUpdate;

/**
 * Builder for incremental construction of {@link TermedStatementDocumentUpdate}
 * objects.
 */
public abstract class TermedStatementDocumentUpdateBuilder extends LabeledStatementDocumentUpdateBuilder {

	TermUpdate descriptions = TermUpdate.NULL;
	final Map<String, List<MonolingualTextValue>> aliases = new HashMap<>();

	/**
	 * Initializes new builder object for constructing update of entity with given
	 * ID.
	 * 
	 * @param entityId
	 *            ID of the entity that is to be updated
	 * @throws NullPointerException
	 *             if {@code entityId} is {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code entityId} is a placeholder ID
	 */
	protected TermedStatementDocumentUpdateBuilder(EntityIdValue entityId) {
		super(entityId);
	}

	/**
	 * Initializes new builder object for constructing update of given base entity
	 * revision.
	 * 
	 * @param revision
	 *            base entity revision to be updated
	 * @throws NullPointerException
	 *             if {@code revision} is {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code revision} has placeholder ID
	 */
	protected TermedStatementDocumentUpdateBuilder(TermedStatementDocument revision) {
		super(revision);
	}

	/**
	 * Creates new builder object for constructing update of entity with given ID.
	 * <p>
	 * Supported entity IDs include {@link ItemIdValue} and {@link PropertyIdValue}.
	 * 
	 * @param entityId
	 *            ID of the entity that is to be updated
	 * @return builder object matching entity type
	 * @throws NullPointerException
	 *             if {@code entityId} is {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code entityId} is of unrecognized type or it is a
	 *             placeholder ID
	 */
	public static TermedStatementDocumentUpdateBuilder forEntityId(EntityIdValue entityId) {
		Objects.requireNonNull(entityId, "Entity ID cannot be null.");
		if (entityId instanceof ItemIdValue) {
			return ItemUpdateBuilder.forEntityId((ItemIdValue) entityId);
		}
		if (entityId instanceof PropertyIdValue) {
			return PropertyUpdateBuilder.forEntityId((PropertyIdValue) entityId);
		}
		throw new IllegalArgumentException("Unrecognized entity ID type.");
	}

	/**
	 * Creates new builder object for constructing update of given base entity
	 * revision. Provided entity document might not represent the latest revision of
	 * the entity as currently stored in Wikibase. It will be used for validation in
	 * builder methods. If the document has revision ID, it will be used to detect
	 * edit conflicts.
	 * <p>
	 * Supported entity types include {@link ItemDocument} and
	 * {@link PropertyDocument}.
	 * 
	 * @param revision
	 *            base entity revision to be updated
	 * @return builder object matching entity type
	 * @throws NullPointerException
	 *             if {@code revision} is {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code revision} is of unrecognized type or its ID is a
	 *             placeholder ID
	 */
	public static TermedStatementDocumentUpdateBuilder forBaseRevision(TermedStatementDocument revision) {
		Objects.requireNonNull(revision, "Base entity revision cannot be null.");
		if (revision instanceof ItemDocument) {
			return ItemUpdateBuilder.forBaseRevision((ItemDocument) revision);
		}
		if (revision instanceof PropertyDocument) {
			return PropertyUpdateBuilder.forBaseRevision((PropertyDocument) revision);
		}
		throw new IllegalArgumentException("Unrecognized entity document type.");
	}

	@Override
	TermedStatementDocument getBaseRevision() {
		return (TermedStatementDocument) super.getBaseRevision();
	}

	@Override
	public TermedStatementDocumentUpdateBuilder updateStatements(StatementUpdate update) {
		super.updateStatements(update);
		return this;
	}

	@Override
	public TermedStatementDocumentUpdateBuilder updateLabels(TermUpdate update) {
		super.updateLabels(update);
		return this;
	}

	/**
	 * Updates entity descriptions. If this method is called multiple times, changes
	 * are accumulated. If base entity revision was provided, redundant changes are
	 * silently ignored, resulting in empty update.
	 * 
	 * @param update
	 *            changes in entity descriptions
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code update} is {@code null}
	 */
	public TermedStatementDocumentUpdateBuilder updateDescriptions(TermUpdate update) {
		Objects.requireNonNull(update, "Update cannot be null.");
		TermUpdateBuilder combined = getBaseRevision() != null
				? TermUpdateBuilder.forTerms(getBaseRevision().getDescriptions().values())
				: TermUpdateBuilder.create();
		combined.apply(descriptions);
		combined.apply(update);
		descriptions = combined.build();
		return this;
	}

	/**
	 * Updates entity aliases. Any previous aliases for the language code are
	 * discarded. To remove aliases for some language code, pass in empty alias
	 * list. If base entity revision was provided, redundant changes are silently
	 * ignored, resulting in empty update.
	 * 
	 * @param language
	 *            language code of the altered aliases
	 * @param aliases
	 *            new list of aliases for the language, possibly empty
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code language}, {@code aliases}, or any of the aliases are
	 *             {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code aliases} contains duplicates
	 */
	public TermedStatementDocumentUpdateBuilder setAliases(String language, List<String> aliases) {
		Objects.requireNonNull(language, "Language code cannot be null.");
		Objects.requireNonNull(aliases, "Alias list cannot be null.");
		Validate.noNullElements(aliases, "Aliases cannot be null.");
		Validate.isTrue(new HashSet<>(aliases).size() == aliases.size(), "Aliases must be unique.");
		List<MonolingualTextValue> values = aliases.stream()
				.map(a -> Datamodel.makeMonolingualTextValue(a, language))
				.collect(toList());
		if (getBaseRevision() != null) {
			List<MonolingualTextValue> original = getBaseRevision().getAliases().get(language);
			if (values.equals(original) || original == null && values.isEmpty()) {
				this.aliases.remove(language);
				return this;
			}
		}
		this.aliases.put(language, values);
		return this;
	}

	void apply(TermedStatementDocumentUpdate update) {
		super.apply(update);
		updateDescriptions(update.getDescriptions());
		for (Map.Entry<String, List<MonolingualTextValue>> entry : update.getAliases().entrySet()) {
			setAliases(entry.getKey(), entry.getValue().stream().map(v -> v.getText()).collect(toList()));
		}
	}

	/**
	 * Creates new {@link TermedStatementDocumentUpdate} object with contents of
	 * this builder object.
	 * 
	 * @return constructed object
	 */
	@Override
	public abstract TermedStatementDocumentUpdate build();

}
