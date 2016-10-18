/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.perl.lexer.adapters;

import com.intellij.lexer.FlexAdapter;
import com.intellij.openapi.project.Project;
import com.perl5.lang.perl.lexer.PerlLexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by hurricup on 10.10.2015.
 */
public class PerlSubLexerAdapter extends FlexAdapter
{
	private final int myPerlLexerState;

	public PerlSubLexerAdapter(@Nullable Project project, int perlLexerState)
	{
		super(new PerlLexer(project));
		myPerlLexerState = perlLexerState;
	}

	public static PerlSubLexerAdapter forCode(@Nullable Project project)
	{
		return new PerlSubLexerAdapter(project, PerlLexer.YYINITIAL);
	}

	public static PerlSubLexerAdapter forStringSQ(@Nullable Project project)
	{
		return new PerlSubLexerAdapter(project, PerlLexer.LEX_STRING_CONTENT);
	}

	public static PerlSubLexerAdapter forStringDQ(@Nullable Project project)
	{
		return new PerlSubLexerAdapter(project, PerlLexer.LEX_STRING_CONTENT_QQ);
	}

	public static PerlSubLexerAdapter forStringXQ(@Nullable Project project)
	{
		return new PerlSubLexerAdapter(project, PerlLexer.LEX_STRING_CONTENT_XQ);
	}

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
	{
		super.start(buffer, startOffset, endOffset, myPerlLexerState);
	}
}