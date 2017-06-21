/*
 * Copyright 2015-2017 Alexandr Evstigneev
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

package com.perl5.lang.perl.parser.Class.Accessor.psi.impl;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.perl5.lang.perl.extensions.PerlCompletionElementsProvider;
import com.perl5.lang.perl.extensions.PerlRenameUsagesSubstitutor;
import com.perl5.lang.perl.idea.completion.util.PerlSubCompletionUtil;
import com.perl5.lang.perl.parser.Class.Accessor.ClassAccessorElementTypes;
import com.perl5.lang.perl.parser.Class.Accessor.psi.PerlClassAccessorFollowBestPractice;
import com.perl5.lang.perl.parser.Class.Accessor.psi.stubs.PerlClassAccessorDeclarationStub;
import com.perl5.lang.perl.psi.PerlNamespaceDefinitionElement;
import com.perl5.lang.perl.psi.PerlNamespaceDefinitionWithIdentifier;
import com.perl5.lang.perl.psi.PsiPerlNestedCall;
import com.perl5.lang.perl.psi.impl.PerlSubDefinitionWithTextIdentifier;
import com.perl5.lang.perl.psi.stubs.subsdefinitions.PerlSubDefinitionStub;
import com.perl5.lang.perl.util.PerlPackageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by hurricup on 21.01.2016.
 */
public class PerlClassAccessorDeclaration extends PerlSubDefinitionWithTextIdentifier implements ClassAccessorElementTypes,
                                                                                                 PerlCompletionElementsProvider,
                                                                                                 PerlRenameUsagesSubstitutor {
  public static final String ACCESSOR_PREFIX = "get_";
  public static final String MUTATOR_PREFIX = "set_";

  private static final TokenSet READABLE_DECLARATORS = TokenSet.create(
    RESERVED_MK_ACCESSORS,
    RESERVED_MK_RO_ACCESSORS
  );

  private static final TokenSet WRITABLE_DECLARATORS = TokenSet.create(
    RESERVED_MK_ACCESSORS,
    RESERVED_MK_WO_ACCESSORS
  );

  public PerlClassAccessorDeclaration(@NotNull ASTNode node) {
    super(node);
  }

  public PerlClassAccessorDeclaration(@NotNull PerlSubDefinitionStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public boolean isFollowsBestPractice() {
    PerlClassAccessorDeclarationStub stub = (PerlClassAccessorDeclarationStub)getStub();
    if (stub != null) {
      return stub.isFollowsBestPractice();
    }

    // fixme here should be some walking up visitor
    PerlNamespaceDefinitionElement namespaceContainer = PerlPackageUtil.getNamespaceContainerForElement(this);

    return namespaceContainer != null && getFBPElement(namespaceContainer.getFirstChild(), this) != null;
  }

  @Nullable
  protected PerlClassAccessorFollowBestPractice getFBPElement(PsiElement element, PsiElement beforeElement) {
    if (element == null) {
      return null;
    }

    PsiElement currentElement = element;
    while (currentElement != null && currentElement.getTextOffset() < beforeElement.getTextOffset()) {
      if (currentElement.getTextOffset() > beforeElement.getTextOffset()) {
        return null;
      }

      if (currentElement instanceof PerlClassAccessorFollowBestPractice) {
        return (PerlClassAccessorFollowBestPractice)currentElement;
      }

      if (!(currentElement instanceof PerlNamespaceDefinitionWithIdentifier) && currentElement.getNode() instanceof CompositeElement) {
        PsiElement subResult = getFBPElement(currentElement.getFirstChild(), beforeElement);
        if (subResult != null) {
          return (PerlClassAccessorFollowBestPractice)subResult;
        }
      }

      currentElement = currentElement.getNextSibling();
    }

    return null;
  }

  public boolean isAccessorReadable() {
    PerlClassAccessorDeclarationStub stub = (PerlClassAccessorDeclarationStub)getStub();
    if (stub != null) {
      return stub.isAccessorReadable();
    }
    return READABLE_DECLARATORS.contains(getDeclaratorElementType());
  }

  public boolean isAccessorWritable() {
    PerlClassAccessorDeclarationStub stub = (PerlClassAccessorDeclarationStub)getStub();
    if (stub != null) {
      return stub.isAccessorWritable();
    }
    return WRITABLE_DECLARATORS.contains(getDeclaratorElementType());
  }

  @Nullable
  protected IElementType getDeclaratorElementType() {
    PsiPerlNestedCall nestedCall = PsiTreeUtil.getParentOfType(this, PsiPerlNestedCall.class);
    if (nestedCall != null) {
      return nestedCall.getMethod().getFirstChild().getNode().getElementType();
    }
    return null;
  }

  @Override
  public void fillCompletions(CompletionResultSet resultSet) {
    String getterName = getSubName();
    String setterName = getterName;
    if (isFollowsBestPractice()) {
      getterName = getGetterName();
      setterName = getSetterName();
    }

    if (isAccessorReadable()) {
      resultSet.addElement(PerlSubCompletionUtil.getSubDefinitionLookupElement(
        getterName,
        "",
        this
      ));
    }
    if (isAccessorWritable()) {
      resultSet.addElement(PerlSubCompletionUtil.getSubDefinitionLookupElement(
        setterName,
        "($new_value)",
        this
      ));
    }
  }

  // fixme not dry with Stub
  public String getGetterName() {
    return ACCESSOR_PREFIX + getSubName();
  }

  public String getSetterName() {
    return MUTATOR_PREFIX + getSubName();
  }

  @Nullable
  public String getGetterCanonicalName() {
    String packageName = getPackageName();
    if (packageName == null) {
      return null;
    }
    return packageName + PerlPackageUtil.PACKAGE_SEPARATOR + getGetterName();
  }

  @Nullable
  public String getSetterCanonicalName() {
    String packageName = getPackageName();
    if (packageName == null) {
      return null;
    }
    return packageName + PerlPackageUtil.PACKAGE_SEPARATOR + getSetterName();
  }

  @NotNull
  @Override
  public String getSubstitutedUsageName(@NotNull String newName, @NotNull PsiElement element) {
    if (isFollowsBestPractice()) {
      if (getGetterName().equals(element.getText())) {
        return ACCESSOR_PREFIX + newName;
      }
      else if (getSetterName().equals(element.getText())) {
        return MUTATOR_PREFIX + newName;
      }
    }
    return newName;
  }

  @Override
  public String getPresentableName() {
    return getName();
  }
}
