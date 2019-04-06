/*
 * Copyright 2015-2018 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.codeInsight.typeInferrence.value;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

import static com.perl5.lang.perl.idea.codeInsight.typeInferrence.value.PerlValueUnknown.UNKNOWN_VALUE;
import static com.perl5.lang.perl.util.PerlPackageUtil.PACKAGE_ANY;

/**
 * Represents a plain value - string or number
 */
public final class PerlValueStatic extends PerlValue {
  public static final PerlValue ANY_NAMESPACE = PerlValueStatic.create(PACKAGE_ANY);

  @NotNull
  private final String myValue;

  private PerlValueStatic(@NotNull String value) {
    myValue = value;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @NotNull
  @Override
  PerlValueStatic createBlessedCopy(@NotNull PerlValue bless) {
    return this;
  }

  @NotNull
  @Override
  protected Set<String> getSubNames(@NotNull Project project,
                                    @NotNull GlobalSearchScope searchScope,
                                    @Nullable Set<PerlValue> recursion) {
    return Collections.singleton(myValue);
  }

  @NotNull
  @Override
  protected Set<String> getNamespaceNames(@NotNull Project project,
                                          @NotNull GlobalSearchScope searchScope,
                                          @Nullable Set<PerlValue> recursion) {
    return Collections.singleton(myValue);
  }

  @Override
  public boolean canRepresentNamespace(@Nullable String namespaceName) {
    return myValue.equals(namespaceName);
  }

  @Override
  public boolean canRepresentSubName(@Nullable String subName) {
    return myValue.equals(subName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PerlValueStatic value = (PerlValueStatic)o;

    return myValue.equals(value.myValue);
  }

  @Override
  public int hashCode() {
    return myValue.hashCode();
  }

  @NotNull
  public static PerlValue create(@Nullable String value) {
    return value == null ? UNKNOWN_VALUE : new PerlValueStatic(value);
  }

  @Contract("null->null; !null->!null")
  @Nullable
  public static PerlValue createOrNull(@Nullable String value) {
    return value == null ? null : new PerlValueStatic(value);
  }

  @Override
  public String toString() {
    return "Value: " + myValue;
  }
}