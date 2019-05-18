/*
 * Copyright 2015-2019 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.codeInsight.typeInference.value;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.perl5.lang.perl.psi.utils.PerlContextType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValue.PerlValueType.DEFERRED;
import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValue.PerlValueType.DETERMINISTIC;
import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValues.UNDEF_VALUE;
import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValues.UNKNOWN_VALUE;

/**
 * Parent for all perl values
 */
public abstract class PerlValue {

  // transient cached values
  private volatile int myHashCode = 0;
  private volatile PerlValueType myValueDeterminism = null;
  private volatile PerlValue myScalarRepresentation = null;

  protected PerlValue() {
  }

  protected PerlValue(@NotNull StubInputStream dataStream) throws IOException {
  }

  /**
   * @return set of package name from the index, conforming current type
   */
  @NotNull
  public Set<String> getNamespaceNames() {
    return Collections.emptySet();
  }


  /**
   * @return a current value resolved in the context of the {@code contextElement}.
   */
  @NotNull
  public final PerlValue resolve(@NotNull PsiElement contextElement) {
    return new PerlSimpleValueResolver(contextElement).resolve(this);
  }

  /**
   * @return current value resolved in context of the {@code contextElement}
   * @apiNote DO NOT use this method directly, use {@link #resolve(PsiElement)} (Project)}
   * @implSpec feel free to use indexes, resolve and any heavy activity you need
   */
  @NotNull
  abstract PerlValue computeResolve(@NotNull PerlValueResolver resolver);

  /**
   * @return set of sub names which may be represented by the current value
   */
  @NotNull
  public Set<String> getSubNames() {
    return Collections.emptySet();
  }

  public final boolean isUnknown() {
    return this == UNKNOWN_VALUE;
  }

  public final boolean isUndef() {
    return this == UNDEF_VALUE;
  }

  /**
   * @return true iff this type can represent a {@code namespaceName}
   */
  public boolean canRepresentNamespace(@Nullable String namespaceName) {
    return false;
  }

  public boolean canRepresentSubName(@Nullable String subName) {
    return false;
  }

  /**
   * @return a serialization id unique for this value.
   * @see PerlValuesManager
   */
  protected abstract int getSerializationId();

  /**
   * Serializes this value data
   */
  public final void serialize(@NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeVarInt(getSerializationId());
    serializeData(dataStream);
  }

  protected abstract void serializeData(@NotNull StubOutputStream dataStream) throws IOException;

  /**
   * @return a context type for this value. Or null if context can't be determined (can be any)
   */
  @Nullable
  protected abstract PerlContextType getContextType();

  /**
   * @return the scalar representation of the value
   */
  @NotNull
  public final PerlValue getScalarRepresentation() {
    if (isUnknown()) {
      return UNKNOWN_VALUE;
    }
    if (getContextType() == PerlContextType.SCALAR) {
      return this;
    }
    if (isDeterministic()) {
      if (myScalarRepresentation == null) {
        myScalarRepresentation = computeScalarRepresentation();
      }
      return myScalarRepresentation;
    }
    return new PerlScalarContextValue(this);
  }

  @NotNull
  public List<PerlValue> getListRepresentation() {
    return Collections.singletonList(this);
  }

  @NotNull
  protected PerlValue computeScalarRepresentation() {
    throw new RuntimeException("This method must be implemented for determined non-scalar values");
  }

  /**
   * @return true iff this value is deterministic and don't need to be computed
   */
  public final boolean isDeterministic() {
    if (myValueDeterminism == null) {
      myValueDeterminism = computeIsDeterministic() ? DETERMINISTIC : DEFERRED;
    }
    return myValueDeterminism == DETERMINISTIC;
  }

  /**
   * Don't use this method directly, use cached version: {@link #isDeterministic()}
   *
   * @see #isDeterministic()
   */
  protected boolean computeIsDeterministic() {
    return false;
  }

  /**
   * @return code representation, that may be used e.g. in annotation
   */
  public String toCode() {
    return toString();
  }

  /**
   * @return presentable text for tooltips
   */
  @NotNull
  public String getPresentableText() {
    return toString();
  }

  /**
   * @return a value representing scalar dereference of the current element
   */
  @NotNull
  public final PerlValue getScalarDereference() {
    return PerlValuesBuilder.convert(this, PerlValue::createScalarDereference);
  }

  @NotNull
  protected PerlValue createScalarDereference() {
    return new PerlScalarDereferenceValue(this);
  }

  /**
   * @return a value representing an array slice of current element
   */
  @NotNull
  public final PerlValue getArraySlice(@NotNull PerlValue indexesValue) {
    if (indexesValue.isUndef() || indexesValue.isUnknown()) {
      return UNKNOWN_VALUE;
    }
    return createArraySlice(indexesValue);
  }

  @NotNull
  protected PerlValue createArraySlice(@NotNull PerlValue indexesValue) {
    return new PerlArraySliceValue(this, indexesValue);
  }

  /**
   * @return a value representing a hash slice of the current element
   */
  @NotNull
  public final PerlValue getHashSlice(@NotNull PerlValue keysValue) {
    if (keysValue.isUndef() || keysValue.isUnknown()) {
      return UNKNOWN_VALUE;
    }
    return createHashSlice(keysValue);
  }

  @NotNull
  protected PerlValue createHashSlice(@NotNull PerlValue keysValue) {
    return new PerlHashSliceValue(this, keysValue);
  }

  /**
   * @return a value representing hash element from current value.
   */
  @NotNull
  public final PerlValue getHashElement(@NotNull PerlValue hashKey) {
    if (hashKey.isUnknown() || hashKey.isUndef()) {
      return UNKNOWN_VALUE;
    }
    return PerlValuesBuilder.convert(this, it -> it.createHashElement(hashKey));
  }

  @NotNull
  protected PerlValue createHashElement(@NotNull PerlValue hashKey) {
    return new PerlHashElementValue(this, hashKey);
  }

  @Override
  public final int hashCode() {
    return myHashCode != 0 ? myHashCode : (myHashCode = computeHashCode());
  }

  protected int computeHashCode() {
    return getClass().hashCode();
  }

  enum PerlValueType {
    DETERMINISTIC,
    DEFERRED
  }
}
