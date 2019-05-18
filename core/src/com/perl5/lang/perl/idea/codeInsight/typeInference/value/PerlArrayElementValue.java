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

import com.intellij.psi.stubs.StubInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValues.UNKNOWN_VALUE;
import static com.perl5.lang.perl.idea.codeInsight.typeInference.value.PerlValuesManager.ARRAY_ELEMENT_ID;

public final class PerlArrayElementValue extends PerlParametrizedOperationValue {
  PerlArrayElementValue(@NotNull PerlValue baseValue, @NotNull PerlValue index) {
    super(baseValue, index);
  }

  PerlArrayElementValue(@NotNull StubInputStream dataStream) throws IOException {
    super(dataStream);
  }

  @NotNull
  @Override
  protected PerlValue computeResolve(@NotNull PerlValue resolvedArrayValue,
                                     @NotNull PerlValue resolvedIndexValue,
                                     @NotNull PerlValueResolver resolver) {
    return resolvedArrayValue instanceof PerlArrayValue ? ((PerlArrayValue)resolvedArrayValue).get(resolvedIndexValue) : UNKNOWN_VALUE;
  }

  @Override
  protected int getSerializationId() {
    return ARRAY_ELEMENT_ID;
  }

  public PerlValue getArray() {
    return getBaseValue();
  }

  @NotNull
  public PerlValue getIndex() {
    return getParameter();
  }

  @Override
  public String toString() {
    return "ArrayItem: " + getBaseValue() + "[" + getParameter() + "]";
  }

  public static PerlValue create(@NotNull PerlValue listValue, @NotNull PerlValue indexValue) {
    if (listValue.isUnknown() || listValue.isUndef() || indexValue.isUnknown() || indexValue.isUndef()) {
      return UNKNOWN_VALUE;
    }
    return new PerlArrayElementValue(listValue, indexValue);
  }
}
