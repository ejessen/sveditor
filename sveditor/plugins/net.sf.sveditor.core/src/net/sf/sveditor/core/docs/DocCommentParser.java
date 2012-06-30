/****************************************************************************
 * Copyright (c) 2008-2010 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Natural Docs - initial implementation
 *     Armond Paiva - repurposed from Natural Docs for use in SVEditor
 *    
 * This class is largely a Java port of the natural docs native format 
 * parser. The following Natural Docs(ND) Perl packages were 
 * ported in varying degrees:
 * 		NaturalDocs::Parser, NaturalDocs::Parser::Native, 
 * 		NaturalDocs::Parser::ParsedTopic, NaturalDocs::NDMarkup
 *     
 ****************************************************************************
 * Original Natural Docs License:
 *
 *	This file is part of Natural Docs, which is Copyright (c) 2003-2010 Greg Valure
 *	Natural Docs is licensed under version 3 of the GNU Affero General Public License (AGPL)
 *	Refer to License.txt for the complete details
 *	
 ****************************************************************************/

package net.sf.sveditor.core.docs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.sveditor.core.Tuple;
import net.sf.sveditor.core.docs.model.DocItemType;
import net.sf.sveditor.core.docs.model.DocTopic;
import net.sf.sveditor.core.log.ILogLevel;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class DocCommentParser implements IDocCommentParser {
	
	private static Pattern fIsDocCommentPattern ;
	
	private LogHandle fLog ;
	
	// FIXME: this should be replaced by something along the lines of the Topic interface from ND
	
	static {
		fIsDocCommentPattern = Pattern.compile(
						"("
					+		"class"
					+   	"|task"
					+   	"|function"
					+   ")\\s*:\\s*(\\w+)",
			Pattern.CASE_INSENSITIVE) ;
	}
	
	public DocCommentParser() {
		fLog = LogFactory.getLogHandle("DocCommentParser") ;
	}

	public String isDocComment(String comment) {
		Matcher matcher = fIsDocCommentPattern.matcher(comment) ;
		if(matcher.find()) {
			return matcher.group(2) ;
		} else {
			return null ;
		}
	}

	public void parse(String comment, Set<DocTopic> docTopics) {
		
		String lines[] = comment.split("\\r?\\n") ;
		
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		fLog.debug(ILogLevel.LEVEL_MID, "Parsing the following comment:") ;
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		for(String line: lines) { fLog.debug(ILogLevel.LEVEL_MID, line + "<END>") ; }
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		
		cleanComment(lines) ;
		
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		fLog.debug(ILogLevel.LEVEL_MID, "Cleaned the following comment:") ;
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		for(String line: lines) { fLog.debug(ILogLevel.LEVEL_MID, line + "<END>") ; }
		fLog.debug(ILogLevel.LEVEL_MID, "----------------------------------------") ;
		
		parseComment(lines, docTopics) ;
		
//	        {  return NaturalDocs::Parser::Native->ParseComment($commentLines, $isJavaDoc, $lineNumber, \@parsedFile);  }
		
	}
	
//	
//	###############################################################################
//	# Group: Support Functions
//	
//	
//	#   Function: Parse
//	#
//	#   Opens the source file and parses process.  Most of the actual parsing is done in <NaturalDocs::Languages::Base->ParseFile()>
//	#   and <OnComment()>, though.
//	#
//	#   *Do not call externally.*  Rather, call <ParseForInformation()> or <ParseForBuild()>.
//	#
//	#   Returns:
//	#
//	#       The default menu title of the file.  Will be the <FileName> if nothing better is found.
//	#
//	sub Parse
//	    {
//	    my ($self) = @_;
//	
//	    NaturalDocs::Error->OnStartParsing($sourceFile);
//	
//	    $language = NaturalDocs::Languages->LanguageOf($sourceFile);
//	    NaturalDocs::Parser::Native->Start();
//	    @parsedFile = ( );
//	
//	    my ($autoTopics, $scopeRecord) = $language->ParseFile($sourceFile, \@parsedFile);
//	
//	
//	    $self->AddToClassHierarchy();
//	
//	    $self->BreakLists();
//	
//	    if (defined $autoTopics)
//	        {
//	        if (defined $scopeRecord)
//	            {  $self->RepairPackages($autoTopics, $scopeRecord);  };
//	
//	        $self->MergeAutoTopics($language, $autoTopics);
//	        };
//	
//	    $self->RemoveRemainingHeaderlessTopics();
//	
//	
//	    # We don't need to do this if there aren't any auto-topics because the only package changes would be implied by the comments.
//	    if (defined $autoTopics)
//	        {  $self->AddPackageDelineators();  };
//	
//	    if (!NaturalDocs::Settings->NoAutoGroup())
//	        {  $self->MakeAutoGroups($autoTopics);  };
//	
//	
//	    # Set the menu title.
//	
//	    my $defaultMenuTitle = $sourceFile;
//	
//	    if (scalar @parsedFile)
//	        {
//	        my $addFileTitle;
//	
//	        if (NaturalDocs::Settings->OnlyFileTitles())
//	            {
//	            # We still want to use the title from the topics if the first one is a file.
//	            if ($parsedFile[0]->Type() eq ::TOPIC_FILE())
//	                {  $addFileTitle = 0;  }
//	            else
//	                {  $addFileTitle = 1;  };
//	            }
//	        elsif (scalar @parsedFile == 1 || NaturalDocs::Topics->TypeInfo( $parsedFile[0]->Type() )->PageTitleIfFirst())
//	            {  $addFileTitle = 0;  }
//	        else
//	            {  $addFileTitle = 1;  };
//	
//	        if (!$addFileTitle)
//	            {
//	            $defaultMenuTitle = $parsedFile[0]->Title();
//	            }
//	        else
//	            {
//	            # If the title ended up being the file name, add a leading section for it.
//	
//	            unshift @parsedFile,
//	                       NaturalDocs::Parser::ParsedTopic->New(::TOPIC_FILE(), (NaturalDocs::File->SplitPath($sourceFile))[2],
//	                                                                                  undef, undef, undef, undef, undef, 1, undef);
//	            };
//	        };
//	
//	    NaturalDocs::Error->OnEndParsing($sourceFile);
//	
//	    return $defaultMenuTitle;
//	    };
//	
//	
//	#
//	#   Function: CleanComment
//	#
//	#   Removes any extraneous formatting and whitespace from the comment.  Eliminates comment boxes, horizontal lines, trailing
//	#   whitespace from lines, and expands all tab characters.  It keeps leading whitespace, though, since it may be needed for
//	#   example code, and blank lines, since the original line numbers are needed.
//	#
	
	private enum Uniformity { DONT_KNOW, IS_UNIFORM, IS_UNIFORM_IF_AT_END, IS_NOT_UNIFORM } ;
	
	private void cleanComment(String[] lines) {
//	
//	    use constant DONT_KNOW => 0;
//	    use constant IS_UNIFORM => 1;
//	    use constant IS_UNIFORM_IF_AT_END => 2;
//	    use constant IS_NOT_UNIFORM => 3;
//	
//	    my $leftSide = DONT_KNOW;
//	    my $rightSide = DONT_KNOW;
//	    my $leftSideChar;
//	    my $rightSideChar;
		
		Uniformity leftSide = Uniformity.DONT_KNOW ;
		Uniformity rightSide = Uniformity.DONT_KNOW ;
		
//	
//	    my $index = 0;
//	    my $tabLength = NaturalDocs::Settings->TabLength();
		
		String tabExpansion = "   " ;
		int index = 0 ;
		boolean inCodeSection = false ;
		
		while(index < lines.length) {
		
//	        # Strip trailing whitespace from the original.
//	
//	        $commentLines->[$index] =~ s/[ \t]+$//;
			
			lines[index] = lines[index].replaceAll("[ \\t]+$", "") ;
			
//	        # Expand tabs in the original.  This method is almost six times faster than Text::Tabs' method.
//	
//	        my $tabIndex = index($commentLines->[$index], "\t");
//	
//	        while ($tabIndex != -1)
//	            {
//	            substr( $commentLines->[$index], $tabIndex, 1, ' ' x ($tabLength - ($tabIndex % $tabLength)) );
//	            $tabIndex = index($commentLines->[$index], "\t", $tabIndex);
//	            };
			
			lines[index] = lines[index].replaceAll("\\n", tabExpansion) ;

//	        # Make a working copy and strip leading whitespace as well.  This has to be done after tabs are expanded because
//	        # stripping indentation could change how far tabs are expanded.
//	
//	        my $line = $commentLines->[$index];
//	        $line =~ s/^ +//;
			
			String line = lines[index] ;
			
	        // If the line is blank...
			//
			if(line.length()==0) {
	            // If we have a potential vertical line, this only acceptable if it's at the end of the comment.
	            if (leftSide == Uniformity.IS_UNIFORM)
	                {  leftSide = Uniformity.IS_UNIFORM_IF_AT_END ; }
	            if (rightSide == Uniformity.IS_UNIFORM)
	                {  rightSide = Uniformity.IS_UNIFORM_IF_AT_END ; }
            }

	        // If there's at least four symbols in a row, it's a horizontal line.  The second regex supports differing edge characters.  It
	        // doesn't matter if any of this matches the left and right side symbols.  The length < 256 is a sanity check, because that
	        // regexp has caused the perl regexp engine to choke on an insane line someone sent me from an automatically generated
	        // file.  It had over 10k characters on the first line, and most of them were 0x00.
			//
			else if (line.matches("^([^a-zA-Z0-9 ])\\1{3,}$") ||
					 ((line.length() < 256) &&
							 line.matches("^([^a-zA-Z0-9 ])\\1*([^a-zA-Z0-9 ])\\2{3,}([^a-zA-Z0-9 ])\\3*$/"))) {
			
	            // Ignore it.  This has no effect on the vertical line detection.  We want to keep it in the output though in case it was
	            // in a code section.

	        // If the line is not blank or a horizontal line...
			//
			} else {
	        	
	            // More content means any previous blank lines are no longer tolerated in vertical line detection.  They are only
	            // acceptable at the end of the comment.
	
	            if (leftSide == Uniformity.IS_UNIFORM_IF_AT_END)
	                {  leftSide = Uniformity.IS_NOT_UNIFORM;  }
	            if (rightSide == Uniformity.IS_UNIFORM_IF_AT_END)
	                {  rightSide = Uniformity.IS_NOT_UNIFORM;  }


//	            # Detect vertical lines.  Lines are only lines if they are followed by whitespace or a connected horizontal line.
//	            # Otherwise we may accidentally detect lines from short comments that just happen to have every first or last
//	            # character the same.
//	
//	            if ($leftSide != IS_NOT_UNIFORM)
//	                {
//	                if ($line =~ /^([^a-zA-Z0-9])\1*(?: |$)/)
//	                    {
//	                    if ($leftSide == DONT_KNOW)
//	                        {
//	                        $leftSide = IS_UNIFORM;
//	                        $leftSideChar = $1;
//	                        }
//	                    else # ($leftSide == IS_UNIFORM)  Other choices already ruled out.
//	                        {
//	                        if ($leftSideChar ne $1)
//	                            {  $leftSide = IS_NOT_UNIFORM;  };
//	                        };
//	                    }
//	                # We'll tolerate the lack of symbols on the left on the first line, because it may be a
//	                # /* Function: Whatever
//	                #  * Description.
//	                #  */
//	                # comment which would have the leading /* blanked out.
//	                elsif ($index != 0)
//	                    {
//	                    $leftSide = IS_NOT_UNIFORM;
//	                    };
//	                };
//	
//	            if ($rightSide != IS_NOT_UNIFORM)
//	                {
//	                if ($line =~ / ([^a-zA-Z0-9])\1*$/)
//	                    {
//	                    if ($rightSide == DONT_KNOW)
//	                        {
//	                        $rightSide = IS_UNIFORM;
//	                        $rightSideChar = $1;
//	                        }
//	                    else # ($rightSide == IS_UNIFORM)  Other choices already ruled out.
//	                        {
//	                        if ($rightSideChar ne $1)
//	                            {  $rightSide = IS_NOT_UNIFORM;  };
//	                        };
//	                    }
//	                else
//	                    {
//	                    $rightSide = IS_NOT_UNIFORM;
//	                    };
//	                };
//	
//	            # We'll remove vertical lines later if they're uniform throughout the entire comment.
	            
            } 
			
        	index++ ;
        }
	
	
	    if (leftSide == Uniformity.IS_UNIFORM_IF_AT_END)
	        {  leftSide = Uniformity.IS_UNIFORM;  }
	    if (rightSide == Uniformity.IS_UNIFORM_IF_AT_END)
	        {  rightSide = Uniformity.IS_UNIFORM;  }
	
	    index = 0;
	    inCodeSection = false ;
	    
	    while(index < lines.length) {
	    
	        // Clear horizontal lines only if we're not in a code section.
	    	//
	        if (lines[index].matches("^ *([^a-zA-Z0-9 ])\\1{3,}") ||
	            ( lines[index].length() < 256 &&
	              lines[index].matches("^ *([^a-zA-Z0-9 ])\\1*([^a-zA-Z0-9 ])\\2{3,}([^a-zA-Z0-9 ])\\3*$" )))
	        	{
	        	if (!inCodeSection)
	        		{  lines[index] = "" ;  }
	        	}
	
	        else {
		        // Clear vertical lines.
	
		        if (leftSide == Uniformity.IS_UNIFORM) {
		            // This works because every line should either start this way, be blank, or be the first line that doesn't start with a
		            // symbol.
		            lines[index].replace("^ *([^a-zA-Z0-9 ])\\1*","") ;
	            }
	
		        if (rightSide == Uniformity.IS_UNIFORM) {
		            lines[index].replace(" *([^a-zA-Z0-9 ])\\1*$","") ;
	            }
	
		        // Clear horizontal lines again if there were vertical lines.  This catches lines that were separated from the verticals by
		        // whitespace.
	
		        if ((leftSide == Uniformity.IS_UNIFORM || rightSide == Uniformity.IS_UNIFORM) && !inCodeSection) {
		            lines[index].replace("^ *([^a-zA-Z0-9 ])\\1{3,}$","") ;
		            lines[index].replace("^ *([^a-zA-Z0-9 ])\\1*([^a-zA-Z0-9 ])\\2{3,}([^a-zA-Z0-9 ])\\3*$","") ;
	            }
	
		        // Check for the start and end of code sections.  Note that this doesn't affect vertical line removal.
		        //
	        	Pattern patternCodeStart = Pattern.compile("^ *\\( *(?:(?:start|begin)? +)?(?:table|code|example|diagram) *\\)$", Pattern.CASE_INSENSITIVE ) ;
		        Pattern patternCodeEnd = Pattern.compile("^ *\\( *(?:end|finish|done)(?: +(?:table|code|example|diagram))? *\\)$", Pattern.CASE_INSENSITIVE) ;
		        if (!inCodeSection &&
		        		patternCodeStart.matcher(lines[index]).matches()) {
		        	inCodeSection = true ;
	        	}
		        else if (inCodeSection && patternCodeEnd.matcher(lines[index]).matches()) { 
		        	 inCodeSection = false ;
		        }
	
			}
	        index++ ;
		}	
	}
	
//	
//	###############################################################################
//	# Group: Processing Functions
//	
//	
//	#
//	#   Function: RepairPackages
//	#
//	#   Recalculates the packages for all comment topics using the auto-topics and the scope record.  Call this *before* calling
//	#   <MergeAutoTopics()>.
//	#
//	#   Parameters:
//	#
//	#       autoTopics - A reference to the list of automatically generated <NaturalDocs::Parser::ParsedTopics>.
//	#       scopeRecord - A reference to an array of <NaturalDocs::Languages::Advanced::ScopeChanges>.
//	#
//	sub RepairPackages #(autoTopics, scopeRecord)
//	    {
//	    my ($self, $autoTopics, $scopeRecord) = @_;
//	
//	    my $topicIndex = 0;
//	    my $autoTopicIndex = 0;
//	    my $scopeIndex = 0;
//	
//	    my $topic = $parsedFile[0];
//	    my $autoTopic = $autoTopics->[0];
//	    my $scopeChange = $scopeRecord->[0];
//	
//	    my $currentPackage;
//	    my $inFakePackage;
//	
//	    while (defined $topic)
//	        {
//	        # First update the scope via the record if its defined and has the lowest line number.
//	        if (defined $scopeChange &&
//	            $scopeChange->LineNumber() <= $topic->LineNumber() &&
//	            (!defined $autoTopic || $scopeChange->LineNumber() <= $autoTopic->LineNumber()) )
//	            {
//	            $currentPackage = $scopeChange->Scope();
//	            $scopeIndex++;
//	            $scopeChange = $scopeRecord->[$scopeIndex];  # Will be undef when past end.
//	            $inFakePackage = undef;
//	            }
//	
//	        # Next try to end a fake scope with an auto topic if its defined and has the lowest line number.
//	        elsif (defined $autoTopic &&
//	                $autoTopic->LineNumber() <= $topic->LineNumber())
//	            {
//	            if ($inFakePackage)
//	                {
//	                $currentPackage = $autoTopic->Package();
//	                $inFakePackage = undef;
//	                };
//	
//	            $autoTopicIndex++;
//	            $autoTopic = $autoTopics->[$autoTopicIndex];  # Will be undef when past end.
//	            }
//	
//	
//	        # Finally try to handle the topic, since it has the lowest line number.  Check for Type() because headerless topics won't have
//	        # one.
//	        else
//	            {
//	            my $scope;
//	            if ($topic->Type())
//	                {  $scope = NaturalDocs::Topics->TypeInfo($topic->Type())->Scope();  }
//	            else
//	                {  $scope = ::SCOPE_NORMAL();  };
//	
//	            if ($scope == ::SCOPE_START() || $scope == ::SCOPE_END())
//	                {
//	                # They should already have the correct class and scope.
//	                $currentPackage = $topic->Package();
//	                $inFakePackage = 1;
//	                }
//	           else
//	                {
//	                # Fix the package of everything else.
//	
//	                # Note that the first function or variable topic to appear in a fake package will assume that package even if it turns out
//	                # to be incorrect in the actual code, since the topic will come before the auto-topic.  This will be corrected in
//	                # MergeAutoTopics().
//	
//	                $topic->SetPackage($currentPackage);
//	                };
//	
//	            $topicIndex++;
//	            $topic = $parsedFile[$topicIndex];  # Will be undef when past end.
//	            };
//	        };
//	
//	    };
//	
//	
//	#
//	#   Function: MergeAutoTopics
//	#
//	#   Merges the automatically generated topics into the file.  If an auto-topic matches an existing topic, it will have it's prototype
//	#   and package transferred.  If it doesn't, the auto-topic will be inserted into the list unless
//	#   <NaturalDocs::Settings->DocumentedOnly()> is set.  If an existing topic doesn't have a title, it's assumed to be a headerless
//	#   comment and will be merged with the next auto-topic or discarded.
//	#
//	#   Parameters:
//	#
//	#       language - The <NaturalDocs::Languages::Base>-derived class for the file.
//	#       autoTopics - A reference to the list of automatically generated topics.
//	#
//	sub MergeAutoTopics #(language, autoTopics)
//	    {
//	    my ($self, $language, $autoTopics) = @_;
//	
//	    my $topicIndex = 0;
//	    my $autoTopicIndex = 0;
//	
//	    # Keys are topic types, values are existence hashrefs of titles.
//	    my %topicsInLists;
//	
//	    while ($topicIndex < scalar @parsedFile && $autoTopicIndex < scalar @$autoTopics)
//	        {
//	        my $topic = $parsedFile[$topicIndex];
//	        my $autoTopic = $autoTopics->[$autoTopicIndex];
//	
//	        my $cleanTitle = $topic->Title();
//	        $cleanTitle =~ s/[\t ]*\([^\(]*$//;
//	
//	        # Add the auto-topic if it's higher in the file than the current topic.
//	        if ($autoTopic->LineNumber() < $topic->LineNumber())
//	            {
//	            if (exists $topicsInLists{$autoTopic->Type()} &&
//	                exists $topicsInLists{$autoTopic->Type()}->{$autoTopic->Title()})
//	                {
//	                # Remove it from the list so a second one with the same name will be added.
//	                delete $topicsInLists{$autoTopic->Type()}->{$autoTopic->Title()};
//	                }
//	            elsif (!NaturalDocs::Settings->DocumentedOnly())
//	                {
//	                splice(@parsedFile, $topicIndex, 0, $autoTopic);
//	                $topicIndex++;
//	                };
//	
//	            $autoTopicIndex++;
//	            }
//	
//	        # Remove a headerless topic if there's another topic between it and the next auto-topic.
//	        elsif (!$topic->Title() && $topicIndex + 1 < scalar @parsedFile &&
//	                $parsedFile[$topicIndex+1]->LineNumber() < $autoTopic->LineNumber())
//	            {
//	            splice(@parsedFile, $topicIndex, 1);
//	            }
//	
//	        # Transfer information if we have a match or a headerless topic.
//	        elsif ( !$topic->Title() ||
//	        		  $topic->Symbol() eq $autoTopic->Symbol() ||
//	        		  ( $topic->Type() == $autoTopic->Type() &&
//	        			( index($autoTopic->Title(), $cleanTitle) != -1 || index($cleanTitle, $autoTopic->Title()) != -1 ) ) )
//	            {
//	            $topic->SetType($autoTopic->Type());
//	            $topic->SetPrototype($autoTopic->Prototype());
//	            $topic->SetUsing($autoTopic->Using());
//	
//	            if (!$topic->Title())
//	                {  $topic->SetTitle($autoTopic->Title());  };
//	
//	            if (NaturalDocs::Topics->TypeInfo($topic->Type())->Scope() != ::SCOPE_START())
//	                {  $topic->SetPackage($autoTopic->Package());  }
//	            elsif ($autoTopic->Package() ne $topic->Package())
//	                {
//	                my @autoPackageIdentifiers = NaturalDocs::SymbolString->IdentifiersOf($autoTopic->Package());
//	                my @packageIdentifiers = NaturalDocs::SymbolString->IdentifiersOf($topic->Package());
//	
//	                while (scalar @autoPackageIdentifiers && $autoPackageIdentifiers[-1] eq $packageIdentifiers[-1])
//	                    {
//	                    pop @autoPackageIdentifiers;
//	                    pop @packageIdentifiers;
//	                    };
//	
//	                if (scalar @autoPackageIdentifiers)
//	                    {  $topic->SetPackage( NaturalDocs::SymbolString->Join(@autoPackageIdentifiers) );  };
//	                };
//	
//	            $topicIndex++;
//	            $autoTopicIndex++;
//	            }
//	
//	        # Extract topics in lists.
//	        elsif ($topic->IsList())
//	            {
//	            if (!exists $topicsInLists{$topic->Type()})
//	                {  $topicsInLists{$topic->Type()} = { };  };
//	
//	            my $body = $topic->Body();
//	
//	            while ($body =~ /<ds>([^<]+)<\/ds>/g)
//	                {  $topicsInLists{$topic->Type()}->{NaturalDocs::NDMarkup->RestoreAmpChars($1)} = 1;  };
//	
//	            $topicIndex++;
//	            }
//	
//	        # Otherwise there's no match.  Skip the topic.  The auto-topic will be added later.
//	        else
//	            {
//	            $topicIndex++;
//	            }
//	        };
//	
//	    # Add any auto-topics remaining.
//	    if (!NaturalDocs::Settings->DocumentedOnly())
//	    	{
//		    while ($autoTopicIndex < scalar @$autoTopics)
//		        {
//		        my $autoTopic = $autoTopics->[$autoTopicIndex];
//	
//		        if (exists $topicsInLists{$autoTopic->Type()} &&
//		            exists $topicsInLists{$autoTopic->Type()}->{$autoTopic->Title()})
//		            {
//		            # Remove it from the list so a second one with the same name will be added.
//		            delete $topicsInLists{$autoTopic->Type()}->{$autoTopic->Title()};
//		            }
//		        else
//		            {
//		            push(@parsedFile, $autoTopic);
//		            };
//	
//		        $autoTopicIndex++;
//		        };
//	        };
//	   };
//	
//	
//	#
//	#   Function: RemoveRemainingHeaderlessTopics
//	#
//	#   After <MergeAutoTopics()> is done, this function removes any remaining headerless topics from the file.  If they don't merge
//	#   into anything, they're not valid topics.
//	#
//	sub RemoveRemainingHeaderlessTopics
//	    {
//	    my ($self) = @_;
//	
//	    my $index = 0;
//	    while ($index < scalar @parsedFile)
//	        {
//	        if ($parsedFile[$index]->Title())
//	            {  $index++;  }
//	        else
//	            {  splice(@parsedFile, $index, 1);  };
//	        };
//	    };
//	
//	
//	#
//	#   Function: MakeAutoGroups
//	#
//	#   Creates group topics for files that do not have them.
//	#
//	sub MakeAutoGroups
//	    {
//	    my ($self) = @_;
//	
//	    # No groups only one topic.
//	    if (scalar @parsedFile < 2)
//	        {  return;  };
//	
//	    my $index = 0;
//	    my $startStretch = 0;
//	
//	    # Skip the first entry if its the page title.
//	    if (NaturalDocs::Topics->TypeInfo( $parsedFile[0]->Type() )->PageTitleIfFirst())
//	        {
//	        $index = 1;
//	        $startStretch = 1;
//	        };
//	
//	    # Make auto-groups for each stretch between scope-altering topics.
//	    while ($index < scalar @parsedFile)
//	        {
//	        my $scope = NaturalDocs::Topics->TypeInfo($parsedFile[$index]->Type())->Scope();
//	
//	        if ($scope == ::SCOPE_START() || $scope == ::SCOPE_END())
//	            {
//	            if ($index > $startStretch)
//	                {  $index += $self->MakeAutoGroupsFor($startStretch, $index);  };
//	
//	            $startStretch = $index + 1;
//	            };
//	
//	        $index++;
//	        };
//	
//	    if ($index > $startStretch)
//	        {  $self->MakeAutoGroupsFor($startStretch, $index);  };
//	    };
//	
//	
//	#
//	#   Function: MakeAutoGroupsFor
//	#
//	#   Creates group topics for sections of files that do not have them.  A support function for <MakeAutoGroups()>.
//	#
//	#   Parameters:
//	#
//	#       startIndex - The index to start at.
//	#       endIndex - The index to end at.  Not inclusive.
//	#
//	#   Returns:
//	#
//	#       The number of group topics added.
//	#
//	sub MakeAutoGroupsFor #(startIndex, endIndex)
//	    {
//	    my ($self, $startIndex, $endIndex) = @_;
//	
//	    # No groups if any are defined already.
//	    for (my $i = $startIndex; $i < $endIndex; $i++)
//	        {
//	        if ($parsedFile[$i]->Type() eq ::TOPIC_GROUP())
//	            {  return 0;  };
//	        };
//	
//	
//	    use constant COUNT => 0;
//	    use constant TYPE => 1;
//	    use constant SECOND_TYPE => 2;
//	    use constant SIZE => 3;
//	
//	    # This is an array of ( count, type, secondType ) triples.  Count and Type will always be filled in; count is the number of
//	    # consecutive topics of type.  On the second pass, if small groups are combined secondType will be filled in.  There will not be
//	    # more than two types per group.
//	    my @groups;
//	    my $groupIndex = 0;
//	
//	
//	    # First pass: Determine all the groups.
//	
//	    my $i = $startIndex;
//	    my $currentType;
//	
//	    while ($i < $endIndex)
//	        {
//	        if (!defined $currentType || ($parsedFile[$i]->Type() ne $currentType && $parsedFile[$i]->Type() ne ::TOPIC_GENERIC()) )
//	            {
//	            if (defined $currentType)
//	                {  $groupIndex += SIZE;  };
//	
//	            $currentType = $parsedFile[$i]->Type();
//	
//	            $groups[$groupIndex + COUNT] = 1;
//	            $groups[$groupIndex + TYPE] = $currentType;
//	            }
//	        else
//	            {  $groups[$groupIndex + COUNT]++;  };
//	
//	        $i++;
//	        };
//	
//	
//	    # Second pass: Combine groups based on "noise".  Noise means types go from A to B to A at least once, and there are at least
//	    # two groups in a row with three or less, and at least one of those groups is two or less.  So 3, 3, 3 doesn't count as noise, but
//	    # 3, 2, 3 does.
//	
//	    $groupIndex = 0;
//	
//	    # While there are at least three groups left...
//	    while ($groupIndex < scalar @groups - (2 * SIZE))
//	        {
//	        # If the group two places in front of this one has the same type...
//	        if ($groups[$groupIndex + (2 * SIZE) + TYPE] eq $groups[$groupIndex + TYPE])
//	            {
//	            # It means we went from A to B to A, which partially qualifies as noise.
//	
//	            my $firstType = $groups[$groupIndex + TYPE];
//	            my $secondType = $groups[$groupIndex + SIZE + TYPE];
//	
//	            if (NaturalDocs::Topics->TypeInfo($firstType)->CanGroupWith($secondType) ||
//	                NaturalDocs::Topics->TypeInfo($secondType)->CanGroupWith($firstType))
//	                {
//	                my $hasNoise;
//	
//	                my $hasThrees;
//	                my $hasTwosOrOnes;
//	
//	                my $endIndex = $groupIndex;
//	
//	                while ($endIndex < scalar @groups &&
//	                         ($groups[$endIndex + TYPE] eq $firstType || $groups[$endIndex + TYPE] eq $secondType))
//	                    {
//	                    if ($groups[$endIndex + COUNT] > 3)
//	                        {
//	                        # They must be consecutive to count.
//	                        $hasThrees = 0;
//	                        $hasTwosOrOnes = 0;
//	                        }
//	                    elsif ($groups[$endIndex + COUNT] == 3)
//	                        {
//	                        $hasThrees = 1;
//	
//	                        if ($hasTwosOrOnes)
//	                            {  $hasNoise = 1;  };
//	                        }
//	                    else # < 3
//	                        {
//	                        if ($hasThrees || $hasTwosOrOnes)
//	                            {  $hasNoise = 1;  };
//	
//	                        $hasTwosOrOnes = 1;
//	                        };
//	
//	                    $endIndex += SIZE;
//	                    };
//	
//	                if (!$hasNoise)
//	                    {
//	                    $groupIndex = $endIndex - SIZE;
//	                    }
//	                else # hasNoise
//	                    {
//	                    $groups[$groupIndex + SECOND_TYPE] = $secondType;
//	
//	                    for (my $noiseIndex = $groupIndex + SIZE; $noiseIndex < $endIndex; $noiseIndex += SIZE)
//	                        {
//	                        $groups[$groupIndex + COUNT] += $groups[$noiseIndex + COUNT];
//	                        };
//	
//	                    splice(@groups, $groupIndex + SIZE, $endIndex - $groupIndex - SIZE);
//	
//	                    $groupIndex += SIZE;
//	                    };
//	                }
//	
//	            else # They can't group together
//	                {
//	                $groupIndex += SIZE;
//	                };
//	            }
//	
//	        else
//	            {  $groupIndex += SIZE;  };
//	        };
//	
//	
//	    # Finally, create group topics for the parsed file.
//	
//	    $groupIndex = 0;
//	    $i = $startIndex;
//	
//	    while ($groupIndex < scalar @groups)
//	        {
//	        if ($groups[$groupIndex + TYPE] ne ::TOPIC_GENERIC())
//	            {
//	            my $topic = $parsedFile[$i];
//	            my $title = NaturalDocs::Topics->NameOfType($groups[$groupIndex + TYPE], 1);
//	
//	            if (defined $groups[$groupIndex + SECOND_TYPE])
//	                {  $title .= ' and ' . NaturalDocs::Topics->NameOfType($groups[$groupIndex + SECOND_TYPE], 1);  };
//	
//	            splice(@parsedFile, $i, 0, NaturalDocs::Parser::ParsedTopic->New(::TOPIC_GROUP(),
//	                                                                                                            $title,
//	                                                                                                            $topic->Package(), $topic->Using(),
//	                                                                                                            undef, undef, undef,
//	                                                                                                            $topic->LineNumber()) );
//	            $i++;
//	            };
//	
//	        $i += $groups[$groupIndex + COUNT];
//	        $groupIndex += SIZE;
//	        };
//	
//	    return (scalar @groups / SIZE);
//	    };
//	
//	
//	#
//	#   Function: AddToClassHierarchy
//	#
//	#   Adds any class topics to the class hierarchy, since they may not have been called with <OnClass()> if they didn't match up to
//	#   an auto-topic.
//	#
//	sub AddToClassHierarchy
//	    {
//	    my ($self) = @_;
//	
//	    foreach my $topic (@parsedFile)
//	        {
//	        if ($topic->Type() && NaturalDocs::Topics->TypeInfo( $topic->Type() )->ClassHierarchy())
//	            {
//	            if ($topic->IsList())
//	                {
//	                my $body = $topic->Body();
//	
//	                while ($body =~ /<ds>([^<]+)<\/ds>/g)
//	                    {
//	                    $self->OnClass( NaturalDocs::SymbolString->FromText( NaturalDocs::NDMarkup->RestoreAmpChars($1) ) );
//	                    };
//	                }
//	            else
//	                {
//	                $self->OnClass($topic->Package());
//	                };
//	            };
//	        };
//	    };
//	
//	
//	#
//	#   Function: AddPackageDelineators
//	#
//	#   Adds section and class topics to make sure the package is correctly represented in the documentation.  Should be called last in
//	#   this process.
//	#
//	sub AddPackageDelineators
//	    {
//	    my ($self) = @_;
//	
//	    my $index = 0;
//	    my $currentPackage;
//	
//	    # Values are the arrayref [ title, type ];
//	    my %usedPackages;
//	
//	    while ($index < scalar @parsedFile)
//	        {
//	        my $topic = $parsedFile[$index];
//	
//	        if ($topic->Package() ne $currentPackage)
//	            {
//	            $currentPackage = $topic->Package();
//	            my $scopeType = NaturalDocs::Topics->TypeInfo($topic->Type())->Scope();
//	
//	            if ($scopeType == ::SCOPE_START())
//	                {
//	                $usedPackages{$currentPackage} = [ $topic->Title(), $topic->Type() ];
//	                }
//	            elsif ($scopeType == ::SCOPE_END())
//	                {
//	                my $newTopic;
//	
//	                if (!defined $currentPackage)
//	                    {
//	                    $newTopic = NaturalDocs::Parser::ParsedTopic->New(::TOPIC_SECTION(), 'Global',
//	                                                                                                   undef, undef,
//	                                                                                                   undef, undef, undef,
//	                                                                                                   $topic->LineNumber(), undef);
//	                    }
//	                else
//	                    {
//	                    my ($title, $body, $summary, $type);
//	                    my @packageIdentifiers = NaturalDocs::SymbolString->IdentifiersOf($currentPackage);
//	
//	                    if (exists $usedPackages{$currentPackage})
//	                        {
//	                        $title = $usedPackages{$currentPackage}->[0];
//	                        $type = $usedPackages{$currentPackage}->[1];
//	                        $body = '<p>(continued)</p>';
//	                        $summary = '(continued)';
//	                        }
//	                    else
//	                        {
//	                        $title = join($language->PackageSeparator(), @packageIdentifiers);
//	                        $type = ::TOPIC_CLASS();
//	
//	                        # Body and summary stay undef.
//	
//	                        $usedPackages{$currentPackage} = $title;
//	                        };
//	
//	                    my @titleIdentifiers = NaturalDocs::SymbolString->IdentifiersOf( NaturalDocs::SymbolString->FromText($title) );
//	                    for (my $i = 0; $i < scalar @titleIdentifiers; $i++)
//	                        {  pop @packageIdentifiers;  };
//	
//	                    $newTopic = NaturalDocs::Parser::ParsedTopic->New($type, $title,
//	                                                                                                   NaturalDocs::SymbolString->Join(@packageIdentifiers), undef,
//	                                                                                                   undef, $summary, $body,
//	                                                                                                   $topic->LineNumber(), undef);
//	                    }
//	
//	                splice(@parsedFile, $index, 0, $newTopic);
//	                $index++;
//	                }
//	            };
//	
//	        $index++;
//	        };
//	    };
//	



//	###############################################################################
//	#
//	#   Package: NaturalDocs::Parser::Native
//	#
//	###############################################################################
//	#
//	#   A package that converts comments from Natural Docs' native format into <NaturalDocs::Parser::ParsedTopic> objects.
//	#   Unlike most second-level packages, these are packages and not object classes.
//	#
//	###############################################################################
//	
//	# This file is part of Natural Docs, which is Copyright  2003-2010 Greg Valure
//	# Natural Docs is licensed under version 3 of the GNU Affero General Public License (AGPL)
//	# Refer to License.txt for the complete details
//	
//	
//	use strict;
//	use integer;
//	
//	package NaturalDocs::Parser::Native;
//	
//	
//	###############################################################################
//	# Group: Variables
//	
//	
//	# Return values of TagType().  Not documented here.
//	use constant POSSIBLE_OPENING_TAG => 1;
//	use constant POSSIBLE_CLOSING_TAG => 2;
//	use constant NOT_A_TAG => 3;
//	
//	
//	#
//	#   var: package
//	#
//	#   A <SymbolString> representing the package normal topics will be a part of at the current point in the file.  This is a package variable
//	#   because it needs to be reserved between function calls.
//	#
//	my $package;
//	
//	#
//	#   hash: functionListIgnoredHeadings
//	#
//	#   An existence hash of all the headings that prevent the parser from creating function list symbols.  Whenever one of
//	#   these headings are used in a function list topic, symbols are not created from definition lists until the next heading.  The keys
//	#   are in all lowercase.
//	#
//	my %functionListIgnoredHeadings = ( 'parameters' => 1,
//	                                                       'parameter' => 1,
//	                                                       'params' => 1,
//	                                                       'param' => 1,
//	                                                       'arguments' => 1,
//	                                                       'argument' => 1,
//	                                                       'args' => 1,
//	                                                       'arg' => 1 );
//	
//	
//	###############################################################################
//	# Group: Interface Functions
//	


//#
//#   Constants: ScopeType
//#
//#   The possible values for <Scope()>.
//#
//#   SCOPE_NORMAL - The topic stays in the current scope without affecting it.
//#   SCOPE_START - The topic starts a scope.
//#   SCOPE_END - The topic ends a scope, returning it to global.
//#   SCOPE_ALWAYS_GLOBAL - The topic is always global, but it doesn't affect the current scope.
//#
//use constant SCOPE_NORMAL => 1;
//use constant SCOPE_START => 2;
//use constant SCOPE_END => 3;
//use constant SCOPE_ALWAYS_GLOBAL => 4;

//	#
//	sub ParseComment #(commentLines, isJavaDoc, lineNumber, parsedTopics)
//	    {
//	    my ($self, $commentLines, $isJavaDoc, $lineNumber, $parsedTopics) = @_;
//	
	
//	private enum Scope { NONE, NORMAL, START, END, ALWAYS_GLOBAL } ;
	
	private int parseComment(String lines[], Set<DocTopic> parsedTopics) {
	
	    int topicCount = 0 ;
	    boolean prevLineBlank = true ;
	    boolean inCodeSection = false ;

//	    my ($type, $scope, $isPlural, $title, $symbol);
//	    #my $package;  # package variable.
//	    my ($newKeyword, $newTitle);
//	
	    int index = 0 ;
	    
//	    Scope scope = Scope.NONE ;
//	    String topicType = null ;
	    String title = null ;
//	    boolean isPlural = false ;
	
	    int bodyStart = 0 ;
	    int bodyEnd = 0 ; // Note inclusive
	    
	    Tuple<String,String> tupleKeywordTitle = new Tuple<String,String>(null,null) ;

//	    while ($index < scalar @$commentLines)
	    
	    while(index < lines.length ) {
	    
	        // Everything but leading whitespace was removed beforehand.
	    	
	    	// FIXME: move out into a static. No need to recompile the pattern each comment
        	Pattern codeSectionEnd = Pattern.compile("^ *\\( *(?:end|finish|done)(?: +(?:table|code|example|diagram))? *\\)$", Pattern.CASE_INSENSITIVE ) ;

	        if (inCodeSection) {
	            if (codeSectionEnd.matcher(lines[index]).matches()) {  inCodeSection = false ;  }
	
	            prevLineBlank = false ;
	            bodyEnd++ ;
            }
	
	        // If the line is empty...
	        //
	        else if(lines[index].length() == 0) {
	            prevLineBlank = true ;
	            if (topicCount != 0) {  bodyEnd++;  }
            }
	
	        // If the line has a recognized header and the previous line is blank...
//	        else if (prevLineBlank && (($newKeyword, $newTitle) = $self->ParseHeaderLine($commentLines->[$index])) )
	        
	        else if(prevLineBlank && parseHeaderLine(tupleKeywordTitle, lines[index]))
	            {
	        	
	            // Process the previous one, if any.
	
	            if (topicCount != 0) {
	            	
	            	// FIXME: not sure if we need to care about scopes since we have the AST and preproc structures 
	            	
//	                if (scope == Scope.START || scope == Scope.END)
//	                    {  $package = undef;  };
//	
//	                my $body = $self->FormatBody($commentLines, $bodyStart, $bodyEnd, $type, $isPlural);
//	                my $newTopic = $self->MakeParsedTopic($type, $title, $package, $body, $lineNumber + $bodyStart - 1, $isPlural);
//	                push @$parsedTopics, $newTopic;
	            	
	            	String body = formatBody(lines, bodyStart, bodyEnd /* , topicType, isPlural */) ;
	            	
	            	DocTopic newTopic = new DocTopic("todo-name-me",DocItemType.Topic, body, title) ;
	            	
	            	parsedTopics.add(newTopic) ;
	            	
//	                $package = $newTopic->Package();
	            	
                }
	            
	            String keyword = tupleKeywordTitle.first() ;

	            title = tupleKeywordTitle.second() ;
	            
	            fLog.debug(ILogLevel.LEVEL_MID, 
	            		"Found header line for keyword(" 
	            			+ keyword + ") title("
	            			+ title + ")") ;
	            
	            // FIXME: will want to grab keyword and associate it with topic... or something like that

//	            my $typeInfo;
//	            ($type, $typeInfo, $isPlural) = NaturalDocs::Topics->KeywordInfo($newKeyword);
//	            $scope = $typeInfo->Scope();
//	
	            bodyStart = index + 1 ;
	            bodyEnd = index + 1 ;
	
	            topicCount++;
	
	            prevLineBlank = false ;
	        	
            }
	        // If we're on a normal content line within a topic
	        //
	        else if (topicCount != 0) {
	            prevLineBlank = false ;
	            bodyEnd++ ;
	            // FIXME: move pattern out into static... no need to keep recompiline
	            Pattern patternCodeSectionStart = Pattern.compile("^ *\\( *(?:(?:start|begin)? +)?(?:table|code|example|diagram) *\\)$", Pattern.CASE_INSENSITIVE) ;
	            if(patternCodeSectionStart.matcher(lines[index]).matches()) {
	            	inCodeSection = true ;
	            }
	            	
            }
	
	        index++ ;
        }

	    // Last one, if any.  This is the only one that gets the prototypes.
	    //
	    if (topicCount != 0) {
	    	
//	        if ($scope == ::SCOPE_START() || $scope == ::SCOPE_END())
//	            {  $package = undef;  };
//	
//	        my $body = $self->FormatBody($commentLines, $bodyStart, $bodyEnd, $type, $isPlural);
//	        my $newTopic = $self->MakeParsedTopic($type, $title, $package, $body, $lineNumber + $bodyStart - 1, $isPlural);
//	        push @$parsedTopics, $newTopic;
//	        $topicCount++;
	    	
        	String body = formatBody(lines, bodyStart, bodyEnd /* , topicType, isPlural */) ;
        	
        	DocTopic newTopic = new DocTopic("todo-name-me",DocItemType.Topic, body, title) ;
        	
        	parsedTopics.add(newTopic) ;
        	
        	topicCount++ ;
//	
//	        $package = $newTopic->Package();
        }
	    
	    return topicCount ;
		
    }

	@SuppressWarnings("unused")
	private boolean parseHeaderLine(Tuple<String, String> tupleKeywordTitle, String line) {
		
		// FIXME: make static
		Pattern patternHeaderLine = Pattern.compile("^ *([a-z0-9 ]*[a-z0-9]): +(.*)$",Pattern.CASE_INSENSITIVE) ;
		
		Matcher matcher = patternHeaderLine.matcher(line) ;
		
		if(matcher.matches()) {
			
			String keyWord = matcher.group(1) ;
			String title = matcher.group(2) ;
			
			// FIXME: lookup topic.  for now just assume this is a known topic
	
	        // We need to do it this way because if you do "if (ND:T->KeywordInfo($keyword)" and the last element of the array it
	        // returns is false, the statement is false.  That is really retarded, but there it is.
//	        my ($type, undef, undef) = NaturalDocs::Topics->KeywordInfo($keyword);
	
//	        if ($type) {  
			if(true) {
				tupleKeywordTitle.setFirst(keyWord) ;
				tupleKeywordTitle.setSecond(title) ;
	        	return true ;
        	}
	        else {  
	        	return false ;  
        	}
        }
	    else {  
	    	return false ;  
    	}
		
	}
	
//	
//	
//	#
//	#   Function: ParseHeaderLine
//	#
//	#   If the passed line is a topic header, returns the array ( keyword, title ).  Otherwise returns an empty array.
//	#
//	sub ParseHeaderLine #(line)
//	    {
//	    my ($self, $line) = @_;
//	
//	    if ($line =~ /^ *([a-z0-9 ]*[a-z0-9]): +(.*)$/i)
//	        {
//	        my ($keyword, $title) = ($1, $2);
//	
//	        # We need to do it this way because if you do "if (ND:T->KeywordInfo($keyword)" and the last element of the array it
//	        # returns is false, the statement is false.  That is really retarded, but there it is.
//	        my ($type, undef, undef) = NaturalDocs::Topics->KeywordInfo($keyword);
//	
//	        if ($type)
//	            {  return ($keyword, $title);  }
//	        else
//	            {  return ( );  };
//	        }
//	    else
//	        {  return ( );  };
//	    };
//	
//	
//	
//	###############################################################################
//	# Group: Support Functions
//	
//	
//	#
//	#   Function: MakeParsedTopic
//	#
//	#   Creates a <NaturalDocs::Parser::ParsedTopic> object for the passed parameters.  Scope is gotten from
//	#   the package variable <package> instead of from the parameters.  The summary is generated from the body.
//	#
//	#   Parameters:
//	#
//	#       type         - The <TopicType>.  May be undef for headerless topics.
//	#       title          - The title of the topic.  May be undef for headerless topics.
//	#       package    - The package <SymbolString> the topic appears in.
//	#       body        - The topic's body in <NDMarkup>.
//	#       lineNumber - The topic's line number.
//	#       isList         - Whether the topic is a list.
//	#
//	#   Returns:
//	#
//	#       The <NaturalDocs::Parser::ParsedTopic> object.
//	#
//	sub MakeParsedTopic #(type, title, package, body, lineNumber, isList)
//	    {
//	    my ($self, $type, $title, $package, $body, $lineNumber, $isList) = @_;
//	
//	    my $summary;
//	
//	    if (defined $body)
//	        {  $summary = NaturalDocs::Parser->GetSummaryFromBody($body);  };
//	
//	    return NaturalDocs::Parser::ParsedTopic->New($type, $title, $package, undef, undef, $summary,
//	                                                                         $body, $lineNumber, $isList);
//	    };
//	
//	
//	#
//	#    Function: FormatBody
//	#
//	#    Converts the section body to <NDMarkup>.
//	#
//	#    Parameters:
//	#
//	#       commentLines - The arrayref of comment lines.
//	#       startingIndex  - The starting index of the body to format.
//	#       endingIndex   - The ending index of the body to format, *not* inclusive.
//	#       type               - The type of the section.  May be undef for headerless comments.
//	#       isList              - Whether it's a list topic.
//	#
//	#    Returns:
//	#
//	#        The body formatted in <NDMarkup>.
//	#
//	sub FormatBody #(commentLines, startingIndex, endingIndex, type, isList)
//	    {
//	    my ($self, $commentLines, $startingIndex, $endingIndex, $type, $isList) = @_;
	
//	    use constant TAG_NONE => 1;
//	    use constant TAG_PARAGRAPH => 2;
//	    use constant TAG_BULLETLIST => 3;
//	    use constant TAG_DESCRIPTIONLIST => 4;
//	    use constant TAG_HEADING => 5;
//	    use constant TAG_PREFIXCODE => 6;
//	    use constant TAG_TAGCODE => 7;
//	
//	    my %tagEnders = ( TAG_NONE() => '',
//	                                 TAG_PARAGRAPH() => '</p>',
//	                                 TAG_BULLETLIST() => '</li></ul>',
//	                                 TAG_DESCRIPTIONLIST() => '</dd></dl>',
//	                                 TAG_HEADING() => '</h>',
//	                                 TAG_PREFIXCODE() => '</code>',
//	                                 TAG_TAGCODE() => '</code>' );
	
	enum Tag { NONE, PARAGRAPH, BULLETLIST, DESCRIPTIONLIST, HEADING, PREFIXCODE, TAGCODE } ;
	
	private static final Map<Tag,String> fTagEnders ;
	
	static {
		fTagEnders = new HashMap<Tag,String>() ;
		fTagEnders.put(Tag.NONE, "") ;
		fTagEnders.put(Tag.PARAGRAPH, "</p>") ;
		fTagEnders.put(Tag.BULLETLIST, "</li></ul>") ;
		fTagEnders.put(Tag.DESCRIPTIONLIST, "</dd></dl>") ;
		fTagEnders.put(Tag.HEADING, "</h>") ;
		fTagEnders.put(Tag.PREFIXCODE, "</code>") ;
		fTagEnders.put(Tag.TAGCODE, "</code>") ;
	}
	
	private String formatBody(String[] lines, int startIndex, int endIndex) {

		Tag topLevelTag = Tag.NONE ; 
		String output = "" ;
		
	    String textBlock = null ;
	    boolean prevLineBlank = true ;

	    String codeBlock = null ;
	    String removedCodeSpaces ;
	
	    boolean ignoreListSymbols;

	    int index = startIndex ;
	
	    while (index < endIndex) {
	    	
	    
	        // If we're in a tagged code section...
	    	//
	        if (topLevelTag == Tag.TAGCODE) {
	        	
//	            if ($commentLines->[$index] =~ /^ *\( *(?:end|finish|done)(?: +(?:table|code|example|diagram))? *\)$/i)
//	                {
//	                $codeBlock =~ s/\n+$//;
//	                $output .= NaturalDocs::NDMarkup->ConvertAmpChars($codeBlock) . '</code>';
//	                $codeBlock = undef;
//	                $topLevelTag = TAG_NONE;
//	                $prevLineBlank = undef;
//	                }
//	            else
//	                {
//	                $self->AddToCodeBlock($commentLines->[$index], \$codeBlock, \$removedCodeSpaces);
//	                };
	        	
            }
	
//	        # If the line starts with a code designator...
//	        elsif ($commentLines->[$index] =~ /^ *[>:|](.*)$/)
//	            {
	        
	        else if (lines[index].matches("^ *[>:|](.*)$")) {
	        
//	            my $code = $1;
//	
//	            if ($topLevelTag == TAG_PREFIXCODE)
//	                {
//	                $self->AddToCodeBlock($code, \$codeBlock, \$removedCodeSpaces);
//	                }
//	            else # $topLevelTag != TAG_PREFIXCODE
//	                {
//	                if (defined $textBlock)
//	                    {
//	                    $output .= $self->RichFormatTextBlock($textBlock) . $tagEnders{$topLevelTag};
//	                    $textBlock = undef;
//	                    };
//	
//	                $topLevelTag = TAG_PREFIXCODE;
//	                $output .= '<code type="anonymous">';
//	                $self->AddToCodeBlock($code, \$codeBlock, \$removedCodeSpaces);
//	                };
            }

	        // If we're not in either code style...
	        //
	        else {
	        
	            // Strip any leading whitespace.
	        	//
	            lines[index] = lines[index].replaceFirst("^ +","") ;
	
	            // If we were in a prefixed code section...
	            if (topLevelTag == Tag.PREFIXCODE) {

//	                $codeBlock =~ s/\n+$//;
//	                $output .= NaturalDocs::NDMarkup->ConvertAmpChars($codeBlock) . '</code>';
	            	
	                codeBlock = null ;
	                topLevelTag = Tag.NONE;
	                prevLineBlank = false ;
	            	
                }
	
	            // If the line is blank...
	            //
	            if (lines[index].length() == 0) 
	                {
	                // End a paragraph.  Everything else ignores it for now.
	            	//
	                if (topLevelTag == Tag.PARAGRAPH)
	                    {
	                    output += richFormatTextBlock(textBlock) + "</p>" ;
	                    textBlock = null ;
	                    topLevelTag = Tag.NONE;
	                    }
	
	                prevLineBlank = true ;
	                }
//	
//	            # If the line starts with a bullet...
//	            elsif ($commentLines->[$index] =~ /^[-\*o+] +([^ ].*)$/ &&
//	                    substr($1, 0, 2) ne '- ')  # Make sure "o - Something" is a definition, not a bullet.
//	                {
//	                my $bulletedText = $1;
//	
//	                if (defined $textBlock)
//	                    {  $output .= $self->RichFormatTextBlock($textBlock);  };
//	
//	                if ($topLevelTag == TAG_BULLETLIST)
//	                    {
//	                    $output .= '</li><li>';
//	                    }
//	                else #($topLevelTag != TAG_BULLETLIST)
//	                    {
//	                    $output .= $tagEnders{$topLevelTag} . '<ul><li>';
//	                    $topLevelTag = TAG_BULLETLIST;
//	                    };
//	
//	                $textBlock = $bulletedText;
//	
//	                $prevLineBlank = undef;
//	                }
//	
//	            # If the line looks like a description list entry...
//	            elsif ($commentLines->[$index] =~ /^(.+?) +- +([^ ].*)$/ && $topLevelTag != TAG_PARAGRAPH)
//	                {
//	                my $entry = $1;
//	                my $description = $2;
//	
//	                if (defined $textBlock)
//	                    {  $output .= $self->RichFormatTextBlock($textBlock);  };
//	
//	                if ($topLevelTag == TAG_DESCRIPTIONLIST)
//	                    {
//	                    $output .= '</dd>';
//	                    }
//	                else #($topLevelTag != TAG_DESCRIPTIONLIST)
//	                    {
//	                    $output .= $tagEnders{$topLevelTag} . '<dl>';
//	                    $topLevelTag = TAG_DESCRIPTIONLIST;
//	                    };
//	
//	                if (($isList && !$ignoreListSymbols) || $type eq ::TOPIC_ENUMERATION())
//	                    {
//	                    $output .= '<ds>' . NaturalDocs::NDMarkup->ConvertAmpChars($entry) . '</ds><dd>';
//	                    }
//	                else
//	                    {
//	                    $output .= '<de>' . NaturalDocs::NDMarkup->ConvertAmpChars($entry) . '</de><dd>';
//	                    };
//	
//	                $textBlock = $description;
//	
//	                $prevLineBlank = undef;
//	                }
//	
//	            # If the line could be a header...
//	            elsif ($prevLineBlank && $commentLines->[$index] =~ /^(.*)([^ ]):$/)
//	                {
//	                my $headerText = $1 . $2;
//	
//	                if (defined $textBlock)
//	                    {
//	                    $output .= $self->RichFormatTextBlock($textBlock);
//	                    $textBlock = undef;
//	                    }
//	
//	                $output .= $tagEnders{$topLevelTag};
//	                $topLevelTag = TAG_NONE;
//	
//	                $output .= '<h>' . $self->RichFormatTextBlock($headerText) . '</h>';
//	
//	                if ($type eq ::TOPIC_FUNCTION() && $isList)
//	                    {
//	                    $ignoreListSymbols = exists $functionListIgnoredHeadings{lc($headerText)};
//	                    };
//	
//	                $prevLineBlank = undef;
//	                }
//	
//	            # If the line looks like a code tag...
//	            elsif ($commentLines->[$index] =~ /^\( *(?:(?:start|begin)? +)?(table|code|example|diagram) *\)$/i)
//	                {
//					my $codeType = lc($1);
//	
//	                if (defined $textBlock)
//	                    {
//	                    $output .= $self->RichFormatTextBlock($textBlock);
//	                    $textBlock = undef;
//	                    };
//	
//	                if ($codeType eq 'example')
//	                	{  $codeType = 'anonymous';  }
//	                elsif ($codeType eq 'table' || $codeType eq 'diagram')
//	                	{  $codeType = 'text';  }
//	                # else leave it 'code'
//	
//	                $output .= $tagEnders{$topLevelTag} . '<code type="' . $codeType . '">';
//	                $topLevelTag = TAG_TAGCODE;
//	                }
//	
//	            # If the line looks like an inline image...
//	            elsif ($commentLines->[$index] =~ /^(\( *see +)([^\)]+?)( *\))$/i)
//	                {
//	                if (defined $textBlock)
//	                    {
//	                    $output .= $self->RichFormatTextBlock($textBlock);
//	                    $textBlock = undef;
//	                    };
//	
//	                $output .= $tagEnders{$topLevelTag};
//	                $topLevelTag = TAG_NONE;
//	
//	                $output .= '<img mode="inline" target="' . NaturalDocs::NDMarkup->ConvertAmpChars($2) . '" '
//	                                . 'original="' . NaturalDocs::NDMarkup->ConvertAmpChars($1 . $2 . $3) . '">';
//	
//	                $prevLineBlank = undef;
//	                }
//	
	            // If the line isn't any of those, we consider it normal text.
	            //
	            else
	                {
	            	
	                // A blank line followed by normal text ends lists.  We don't handle this when we detect if the line's blank because
	                // we don't want blank lines between list items to break the list.
	            	//
	                if (prevLineBlank && (topLevelTag == Tag.BULLETLIST || topLevelTag == Tag.DESCRIPTIONLIST)) {
	                	
	                    output += richFormatTextBlock(textBlock) + fTagEnders.get(topLevelTag) + "<p>" ;
	
	                    topLevelTag = Tag.PARAGRAPH ;
	                    textBlock = null ;
	                    
                    } else if (topLevelTag == Tag.NONE) {
                    	
	                    output += "<p>";
	                    topLevelTag = Tag.PARAGRAPH ;
	                    // textBlock will already be null
                    }
	            	
	            	if(textBlock != null) { textBlock += " " ; }
	            	else { textBlock = new String() ; }
	            	
	                textBlock += lines[index] ;
	
	                prevLineBlank = false ;
	            	
	                }
	        	
            }
	    	
	        index++ ;
        }
	
	    // Clean up anything left dangling.
	    //
	    if (textBlock != null) {
	        output += richFormatTextBlock(textBlock) + fTagEnders.get(topLevelTag) ;
        } else if (codeBlock != null) {
	        codeBlock.replaceFirst("\n+$","") ;
	        output += convertAmpChars(codeBlock) + "</code>" ;
        }
	
	    return output ;
		
    }
	
//	
//	#
//	#   Function: AddToCodeBlock
//	#
//	#   Adds a line of text to a code block, handling all the indentation processing required.
//	#
//	#   Parameters:
//	#
//	#       line - The line of text to add.
//	#       codeBlockRef - A reference to the code block to add it to.
//	#       removedSpacesRef - A reference to a variable to hold the number of spaces removed.  It needs to be stored between calls.
//	#                                      It will reset itself automatically when the code block codeBlockRef points to is undef.
//	#
//	sub AddToCodeBlock #(line, codeBlockRef, removedSpacesRef)
//	    {
//	    my ($self, $line, $codeBlockRef, $removedSpacesRef) = @_;
//	
//	    $line =~ /^( *)(.*)$/;
//	    my ($spaces, $code) = ($1, $2);
//	
//	    if (!defined $$codeBlockRef)
//	        {
//	        if (length($code))
//	            {
//	            $$codeBlockRef = $code . "\n";
//	            $$removedSpacesRef = length($spaces);
//	            };
//	        # else ignore leading line breaks.
//	        }
//	
//	    elsif (length $code)
//	        {
//	        # Make sure we have the minimum amount of spaces to the left possible.
//	        if (length($spaces) != $$removedSpacesRef)
//	            {
//	            my $spaceDifference = abs( length($spaces) - $$removedSpacesRef );
//	            my $spacesToAdd = ' ' x $spaceDifference;
//	
//	            if (length($spaces) > $$removedSpacesRef)
//	                {
//	                $$codeBlockRef .= $spacesToAdd;
//	                }
//	            else
//	                {
//	                $$codeBlockRef =~ s/^(.)/$spacesToAdd . $1/gme;
//	                $$removedSpacesRef = length($spaces);
//	                };
//	            };
//	
//	        $$codeBlockRef .= $code . "\n";
//	        }
//	
//	    else # (!length $code)
//	        {
//	        $$codeBlockRef .= "\n";
//	        };
//	    };
//	
//	
//	#
//	#   Function: RichFormatTextBlock
//	#
//	#   Applies rich <NDMarkup> formatting to a chunk of text.  This includes both amp chars, formatting tags, and link tags.
//	#
//	#   Parameters:
//	#
//	#       text - The block of text to format.
//	#
//	#   Returns:
//	#
//	#       The formatted text block.
//	#
	
//	sub RichFormatTextBlock #(text)
//	    {
//	    my ($self, $text) = @_;
//	    my $output;
//	
	@SuppressWarnings("unused")
	private String richFormatTextBlock(String text) {
		
		String output = "" ;
		
//	
//	    # First find bare urls, e-mail addresses, and images.  We have to do this before the split because they may contain underscores
//	    # or asterisks.  We have to mark the tags with \x1E and \x1F so they don't get confused with angle brackets from the comment.
//	    # We can't convert the amp chars beforehand because we need lookbehinds in the regexps below and they need to be
//	    # constant length.  Sucks, huh?
//	
//	    $text =~ s{
//	                       # The previous character can't be an alphanumeric or an opening angle bracket.
//	                       (?<!  [a-z0-9<]  )
//	
//	                       # Optional mailto:.  Ignored in output.
//	                       (?:mailto\:)?
//	
//	                       # Begin capture
//	                       (
//	
//	                       # The user portion.  Alphanumeric and - _.  Dots can appear between, but not at the edges or more than
//	                       # one in a row.
//	                       (?:  [a-z0-9\-_]+  \.  )*   [a-z0-9\-_]+
//	
//	                       @
//	
//	                       # The domain.  Alphanumeric and -.  Dots same as above, however, there must be at least two sections
//	                       # and the last one must be two to four alphanumeric characters (.com, .uk, .info, .203 for IP addresses)
//	                       (?:  [a-z0-9\-]+  \.  )+  [a-z]{2,4}
//	
//	                       # End capture.
//	                       )
//	
//	                       # The next character can't be an alphanumeric, which should prevent .abcde from matching the two to
//	                       # four character requirement, or a closing angle bracket.
//	                       (?!  [a-z0-9>]  )
//	
//	                       }
//	
//	                       {"\x1E" . 'email target="' . NaturalDocs::NDMarkup->ConvertAmpChars($1) . '" '
//	                       . 'name="' . NaturalDocs::NDMarkup->ConvertAmpChars($1) . '"' . "\x1F"}igxe;
//	
//	    $text =~ s{
//	                       # The previous character can't be an alphanumeric or an opening angle bracket.
//	                       (?<!  [a-z0-9<]  )
//	
//	                       # Begin capture.
//	                       (
//	
//	                       # URL must start with one of the acceptable protocols.
//	                       (?:http|https|ftp|news|file)\:
//	
//	                       # The acceptable URL characters as far as I know.
//	                       [a-z0-9\-\=\~\@\#\%\&\_\+\/\;\:\?\*\.\,]*
//	
//	                       # The URL characters minus period and comma.  If it ends on them, they're probably intended as
//	                       # punctuation.
//	                       [a-z0-9\-\=\~\@\#\%\&\_\+\/\;\:\?\*]
//	
//	                       # End capture.
//	                       )
//	
//	                       # The next character must not be an acceptable character or a closing angle bracket.  It must also not be a
//						   # dot and then an acceptable character.  These will prevent the URL from ending early just to get a match.
//	                       (?!  \.?[a-z0-9\-\=\~\@\#\%\&\_\+\/\;\:\?\*\>]  )
//	
//	                       }
//	
//	                       {"\x1E" . 'url target="' . NaturalDocs::NDMarkup->ConvertAmpChars($1) . '" '
//	                       . 'name="' . NaturalDocs::NDMarkup->ConvertAmpChars($1) . '"' . "\x1F"}igxe;
//	
//	
//	    # Find image links.  Inline images should already be pulled out by now.
//	
//	    $text =~ s{(\( *see +)([^\)\<\>]+?)( *\))}
//	                      {"\x1E" . 'img mode="link" target="' . NaturalDocs::NDMarkup->ConvertAmpChars($2) . '" '
//	                        . 'original="' . NaturalDocs::NDMarkup->ConvertAmpChars($1 . $2 . $3) . '"' . "\x1F"}gie;
//	
//	
//	
//	    # Split the text from the potential tags.
//	
//	    my @tempTextBlocks = split(/([\*_<>\x1E\x1F])/, $text);
		
		String[] tempTextBlocks = text.split("([\\*_<>\\x1E\\x1F])") ;
		
	
	    // Since the symbols are considered dividers, empty strings could appear between two in a row or at the beginning/end of the
	    // array.  This could seriously screw up TagType(), so we need to get rid of them.
		//
	    ArrayList<String> textBlocks = new ArrayList<String>() ;

///	    while (scalar @tempTextBlocks)
//	        {
//	        my $tempTextBlock = shift @tempTextBlocks;
//	
//	        if (length $tempTextBlock)
//	            {  push @textBlocks, $tempTextBlock;  };
//	        };
	    
	    for(String block: tempTextBlocks) {
	    	if(block.length() != 0) {
	    		textBlocks.add(block) ;
	    	}
	    }
	    
//	    my $bold;
//	    my $underline;
//	    my $underlineHasWhitespace;
//	
	    int index = 0 ;
//	
	    while (index < textBlocks.size()) {
	    	
	    	
//	        if ($textBlocks[$index] eq "\x1E")
	    	if(false) {
	    		
//	            {
//	            $output .= '<';
//	            $index++;
//	
//	            while ($textBlocks[$index] ne "\x1F")
//	                {
//	                $output .= $textBlocks[$index];
//	                $index++;
//	                };
//	
//	            $output .= '>';

	    	}
//	        elsif ($textBlocks[$index] eq '<' && $self->TagType(\@textBlocks, $index) == POSSIBLE_OPENING_TAG)
//	            {
//	            my $endingIndex = $self->ClosingTag(\@textBlocks, $index, undef);
//	
//	            if ($endingIndex != -1)
//	                {
//	                my $linkText;
//	                $index++;
//	
//	                while ($index < $endingIndex)
//	                    {
//	                    $linkText .= $textBlocks[$index];
//	                    $index++;
//	                    };
//	                # Index will be incremented again at the end of the loop.
//	
//	                $linkText = NaturalDocs::NDMarkup->ConvertAmpChars($linkText);
//	
//	                if ($linkText =~ /^(?:mailto\:)?((?:[a-z0-9\-_]+\.)*[a-z0-9\-_]+@(?:[a-z0-9\-]+\.)+[a-z]{2,4})$/i)
//	                    {  $output .= '<email target="' . $1 . '" name="' . $1 . '">';  }
//	                elsif ($linkText =~ /^(.+?) at (?:mailto\:)?((?:[a-z0-9\-_]+\.)*[a-z0-9\-_]+@(?:[a-z0-9\-]+\.)+[a-z]{2,4})$/i)
//	                    {  $output .= '<email target="' . $2 . '" name="' . $1 . '">';  }
//	                elsif ($linkText =~ /^(?:http|https|ftp|news|file)\:/i)
//	                    {  $output .= '<url target="' . $linkText . '" name="' . $linkText . '">';  }
//	                elsif ($linkText =~ /^(.+?) at ((?:http|https|ftp|news|file)\:.+)/i)
//	                    {  $output .= '<url target="' . $2 . '" name="' . $1 . '">';  }
//	                else
//	                    {  $output .= '<link target="' . $linkText . '" name="' . $linkText . '" original="&lt;' . $linkText . '&gt;">';  };
//	                }
//	
//	            else # it's not a link.
//	                {
//	                $output .= '&lt;';
//	                };
//	            }
//	
//	        elsif ($textBlocks[$index] eq '*')
//	            {
//	            my $tagType = $self->TagType(\@textBlocks, $index);
//	
//	            if ($tagType == POSSIBLE_OPENING_TAG && $self->ClosingTag(\@textBlocks, $index, undef) != -1)
//	                {
//	                # ClosingTag() makes sure tags aren't opened multiple times in a row.
//	                $bold = 1;
//	                $output .= '<b>';
//	                }
//	            elsif ($bold && $tagType == POSSIBLE_CLOSING_TAG)
//	                {
//	                $bold = undef;
//	                $output .= '</b>';
//	                }
//	            else
//	                {
//	                $output .= '*';
//	                };
//	            }
//	
//	        elsif ($textBlocks[$index] eq '_')
//	            {
//	            my $tagType = $self->TagType(\@textBlocks, $index);
//	
//	             if ($tagType == POSSIBLE_OPENING_TAG && $self->ClosingTag(\@textBlocks, $index, \$underlineHasWhitespace) != -1)
//	                {
//	                # ClosingTag() makes sure tags aren't opened multiple times in a row.
//	                $underline = 1;
//	                #underlineHasWhitespace is set by ClosingTag().
//	                $output .= '<u>';
//	                }
//	            elsif ($underline && $tagType == POSSIBLE_CLOSING_TAG)
//	                {
//	                $underline = undef;
//	                #underlineHasWhitespace will be reset by the next opening underline.
//	                $output .= '</u>';
//	                }
//	            elsif ($underline && !$underlineHasWhitespace)
//	                {
//	                # If there's no whitespace between underline tags, all underscores are replaced by spaces so
//	                # _some_underlined_text_ becomes <u>some underlined text</u>.  The standard _some underlined text_
//	                # will work too.
//	                $output .= ' ';
//	                }
//	            else
//	                {
//	                $output .= '_';
//	                };
//	            }
//	
	    	//  plain text or a > that isn't part of a link
	        else {
	            output += convertAmpChars(textBlocks.get(index)) ;
	        } ;
	
	    	
	        index++ ;
        } ;
	
	    return output ;
	    
    }

//	
//	#
//	#   Function: TagType
//	#
//	#   Returns whether the tag is a possible opening or closing tag, or neither.  "Possible" because it doesn't check if an opening tag is
//	#   closed or a closing tag is opened, just whether the surrounding characters allow it to be a candidate for a tag.  For example, in
//	#   "A _B" the underscore is a possible opening underline tag, but in "A_B" it is not.  Support function for <RichFormatTextBlock()>.
//	#
//	#   Parameters:
//	#
//	#       textBlocks  - A reference to an array of text blocks.
//	#       index         - The index of the tag.
//	#
//	#   Returns:
//	#
//	#       POSSIBLE_OPENING_TAG, POSSIBLE_CLOSING_TAG, or NOT_A_TAG.
//	#
//	sub TagType #(textBlocks, index)
//	    {
//	    my ($self, $textBlocks, $index) = @_;
//	
//	
//	    # Possible opening tags
//	
//	    if ( ( $textBlocks->[$index] =~ /^[\*_<]$/ ) &&
//	
//	        # Before it must be whitespace, the beginning of the text, or ({["'-/*_.
//	        ( $index == 0 || $textBlocks->[$index-1] =~ /[\ \t\n\(\{\[\"\'\-\/\*\_]$/ ) &&
//	
//	        # Notes for 2.0: Include Spanish upside down ! and ? as well as opening quotes (66) and apostrophes (6).  Look into
//	        # Unicode character classes as well.
//	
//	        # After it must be non-whitespace.
//	        ( $index + 1 < scalar @$textBlocks && $textBlocks->[$index+1] !~ /^[\ \t\n]/) &&
//	
//	        # Make sure we don't accept <<, <=, <-, or *= as opening tags.
//	        ( $textBlocks->[$index] ne '<' || $textBlocks->[$index+1] !~ /^[<=-]/ ) &&
//	        ( $textBlocks->[$index] ne '*' || $textBlocks->[$index+1] !~ /^[\=\*]/ ) &&
//	
//	        # Make sure we don't accept * or _ before it unless it's <.
//	        ( $textBlocks->[$index] eq '<' || $index == 0 || $textBlocks->[$index-1] !~ /[\*\_]$/) )
//	        {
//	        return POSSIBLE_OPENING_TAG;
//	        }
//	
//	
//	    # Possible closing tags
//	
//	    elsif ( ( $textBlocks->[$index] =~ /^[\*_>]$/) &&
//	
//	            # After it must be whitespace, the end of the text, or )}].,!?"';:-/*_.
//	            ( $index + 1 == scalar @$textBlocks || $textBlocks->[$index+1] =~ /^[ \t\n\)\]\}\.\,\!\?\"\'\;\:\-\/\*\_]/ ||
//	              # Links also get plurals, like <link>s, <linx>es, <link>'s, and <links>'.
//	              ( $textBlocks->[$index] eq '>' && $textBlocks->[$index+1] =~ /^(?:es|s|\')/ ) ) &&
//	
//	            # Notes for 2.0: Include closing quotes (99) and apostrophes (9).  Look into Unicode character classes as well.
//	
//	            # Before it must be non-whitespace.
//	            ( $index != 0 && $textBlocks->[$index-1] !~ /[ \t\n]$/ ) &&
//	
//	            # Make sure we don't accept >>, ->, or => as closing tags.  >= is already taken care of.
//	            ( $textBlocks->[$index] ne '>' || $textBlocks->[$index-1] !~ /[>=-]$/ ) &&
//	
//	            # Make sure we don't accept * or _ after it unless it's >.
//	            ( $textBlocks->[$index] eq '>' || $textBlocks->[$index+1] !~ /[\*\_]$/) )
//	        {
//	        return POSSIBLE_CLOSING_TAG;
//	        }
//	
//	    else
//	        {
//	        return NOT_A_TAG;
//	        };
//	
//	    };
//	
//	
//	#
//	#   Function: ClosingTag
//	#
//	#   Returns whether a tag is closed or not, where it's closed if it is, and optionally whether there is any whitespace between the
//	#   tags.  Support function for <RichFormatTextBlock()>.
//	#
//	#   The results of this function are in full context, meaning that if it says a tag is closed, it can be interpreted as that tag in the
//	#   final output.  It takes into account any spoiling factors, like there being two opening tags in a row.
//	#
//	#   Parameters:
//	#
//	#       textBlocks             - A reference to an array of text blocks.
//	#       index                    - The index of the opening tag.
//	#       hasWhitespaceRef  - A reference to the variable that will hold whether there is whitespace between the tags or not.  If
//	#                                     undef, the function will not check.  If the tag is not closed, the variable will not be changed.
//	#
//	#   Returns:
//	#
//	#       If the tag is closed, it returns the index of the closing tag and puts whether there was whitespace between the tags in
//	#       hasWhitespaceRef if it was specified.  If the tag is not closed, it returns -1 and doesn't touch the variable pointed to by
//	#       hasWhitespaceRef.
//	#
//	sub ClosingTag #(textBlocks, index, hasWhitespace)
//	    {
//	    my ($self, $textBlocks, $index, $hasWhitespaceRef) = @_;
//	
//	    my $hasWhitespace;
//	    my $closingTag;
//	
//	    if ($textBlocks->[$index] eq '*' || $textBlocks->[$index] eq '_')
//	        {  $closingTag = $textBlocks->[$index];  }
//	    elsif ($textBlocks->[$index] eq '<')
//	        {  $closingTag = '>';  }
//	    else
//	        {  return -1;  };
//	
//	    my $beginningIndex = $index;
//	    $index++;
//	
//	    while ($index < scalar @$textBlocks)
//	        {
//	        if ($textBlocks->[$index] eq '<' && $self->TagType($textBlocks, $index) == POSSIBLE_OPENING_TAG)
//	            {
//	            # If we hit a < and we're checking whether a link is closed, it's not.  The first < becomes literal and the second one
//	            # becomes the new link opening.
//	            if ($closingTag eq '>')
//	                {
//	                return -1;
//	                }
//	
//	            # If we're not searching for the end of a link, we have to skip the link because formatting tags cannot appear within
//	            # them.  That's of course provided it's closed.
//	            else
//	                {
//	                my $linkHasWhitespace;
//	
//	                my $endIndex = $self->ClosingTag($textBlocks, $index,
//	                                                                    ($hasWhitespaceRef && !$hasWhitespace ? \$linkHasWhitespace : undef) );
//	
//	                if ($endIndex != -1)
//	                    {
//	                    if ($linkHasWhitespace)
//	                        {  $hasWhitespace = 1;  };
//	
//	                    # index will be incremented again at the end of the loop, which will bring us past the link's >.
//	                    $index = $endIndex;
//	                    };
//	                };
//	            }
//	
//	        elsif ($textBlocks->[$index] eq $closingTag)
//	            {
//	            my $tagType = $self->TagType($textBlocks, $index);
//	
//	            if ($tagType == POSSIBLE_CLOSING_TAG)
//	                {
//	                # There needs to be something between the tags for them to count.
//	                if ($index == $beginningIndex + 1)
//	                    {  return -1;  }
//	                else
//	                    {
//	                    # Success!
//	
//	                    if ($hasWhitespaceRef)
//	                        {  $$hasWhitespaceRef = $hasWhitespace;  };
//	
//	                    return $index;
//	                    };
//	                }
//	
//	            # If there are two opening tags of the same type, the first becomes literal and the next becomes part of a tag.
//	            elsif ($tagType == POSSIBLE_OPENING_TAG)
//	                {  return -1;  }
//	            }
//	
//	        elsif ($hasWhitespaceRef && !$hasWhitespace)
//	            {
//	            if ($textBlocks->[$index] =~ /[ \t\n]/)
//	                {  $hasWhitespace = 1;  };
//	            };
//	
//	        $index++;
//	        };
//	
//	    # Hit the end of the text blocks if we're here.
//	    return -1;
//	    };
//	
//	
//	1;

//	###############################################################################
//	#
//	#   Package: NaturalDocs::Parser::ParsedTopic
//	#
//	###############################################################################
//	#
//	#   A class for parsed topics of source files.  Also encompasses some of the <TopicType>-specific behavior.
//	#
//	###############################################################################
//	
//	# This file is part of Natural Docs, which is Copyright  2003-2010 Greg Valure
//	# Natural Docs is licensed under version 3 of the GNU Affero General Public License (AGPL)
//	# Refer to License.txt for the complete details
//	
//	use strict;
//	use integer;
//	
//	package NaturalDocs::Parser::ParsedTopic;
//	
//	
//	###############################################################################
//	# Group: Implementation
//	
//	#
//	#   Constants: Members
//	#
//	#   The object is a blessed arrayref with the following indexes.
//	#
//	#       TYPE           - The <TopicType>.
//	#       TITLE          - The title of the topic.
//	#       PACKAGE    - The package <SymbolString> the topic appears in, or undef if none.
//	#       USING         - An arrayref of additional package <SymbolStrings> available to the topic via "using" statements, or undef if
//	#                           none.
//	#       PROTOTYPE - The prototype, if it exists and is applicable.
//	#       SUMMARY    - The summary, if it exists.
//	#       BODY          - The body of the topic, formatted in <NDMarkup>.  Some topics may not have bodies, and if not, this
//	#                           will be undef.
//	#       LINE_NUMBER  - The line number the topic appears at in the file.
//	#       IS_LIST - Whether the topic is a list.
//	#
//	use NaturalDocs::DefineMembers 'TYPE', 'TITLE', 'PACKAGE', 'USING', 'PROTOTYPE', 'SUMMARY', 'BODY',
//	                                                 'LINE_NUMBER', 'IS_LIST';
//	# DEPENDENCY: New() depends on the order of these constants, and that this class is not inheriting any members.
//	
//	
//	#
//	#   Architecture: Title, Package, and Symbol Behavior
//	#
//	#   Title, package, and symbol behavior is a little awkward so it deserves some explanation.  Basically you set them according to
//	#   certain rules, but you get computed values that try to hide all the different scoping situations.
//	#
//	#   Normal Topics:
//	#
//	#       Set them to the title and package as they appear.  "Function" and "PkgA.PkgB" will return "Function" for the title,
//	#       "PkgA.PkgB" for the package, and "PkgA.PkgB.Function" for the symbol.
//	#
//	#       In the rare case that a title has a separator symbol it's treated as inadvertant, so "A vs. B" in "PkgA.PkgB" still returns just
//	#       "PkgA.PkgB" for the package even though if you got it from the symbol it can be seen as "PkgA.PkgB.A vs".
//	#
//	#   Scope Topics:
//	#
//	#       Set the title normally and leave the package undef.  So "PkgA.PkgB" and undef will return "PkgA.PkgB" for the title as well
//	#       as for the package and symbol.
//	#
//	#       The only time you should set the package is when you have full language support and they only documented the class with
//	#       a partial title.  So if you documented "PkgA.PkgB" with just "PkgB", you want to set the package to "PkgA".  This
//	#       will return "PkgB" as the title for presentation and will return "PkgA.PkgB" for the package and symbol, which is correct.
//	#
//	#   Always Global Topics:
//	#
//	#       Set the title and package normally, do not set the package to undef.  So "Global" and "PkgA.PkgB" will return "Global" as
//	#       the title, "PkgA.PkgB" as the package, and "Global" as the symbol.
//	#
//	#   Um, yeah...:
//	#
//	#       So does this suck?  Yes, yes it does.  But the suckiness is centralized here instead of having to be handled everywhere these
//	#       issues come into play.  Just realize there are a certain set of rules to follow when you *set* these variables, and the results
//	#       you see when you *get* them are computed rather than literal.
//	#
//	
//	
//	###############################################################################
//	# Group: Functions
//	
//	
//	# Function: Type
//	# Returns the <TopicType>.
//	sub Type
//	    {  return $_[0]->[TYPE];  };
//	
//	# Function: SetType
//	# Replaces the <TopicType>.
//	sub SetType #(type)
//	    {  $_[0]->[TYPE] = $_[1];  };
//	
//	# Function: IsList
//	# Returns whether the topic is a list.
//	sub IsList
//	    {  return $_[0]->[IS_LIST];  };
//	
//	# Function: SetIsList
//	# Sets whether the topic is a list.
//	sub SetIsList
//	    {  $_[0]->[IS_LIST] = $_[1];  };
//	
//	# Function: Title
//	# Returns the title of the topic.
//	sub Title
//	    {  return $_[0]->[TITLE];  };
//	
//	# Function: SetTitle
//	# Replaces the topic title.
//	sub SetTitle #(title)
//	    {  $_[0]->[TITLE] = $_[1];  };
//	
//	#
//	#   Function: Symbol
//	#
//	#   Returns the <SymbolString> defined by the topic.  It is fully resolved and does _not_ need to be joined with <Package()>.
//	#
//	#   Type-Specific Behavior:
//	#
//	#       - If the <TopicType> is always global, the symbol will be generated from the title only.
//	#       - Everything else's symbols will be generated from the title and the package passed to <New()>.
//	#
//	sub Symbol
//	    {
//	    my ($self) = @_;
//	
//	    my $titleSymbol = NaturalDocs::SymbolString->FromText($self->[TITLE]);
//	
//	    if (NaturalDocs::Topics->TypeInfo($self->Type())->Scope() == ::SCOPE_ALWAYS_GLOBAL())
//	        {  return $titleSymbol;  }
//	    else
//	        {
//	        return NaturalDocs::SymbolString->Join( $self->[PACKAGE], $titleSymbol );
//	        };
//	    };
//	
//	
//	#
//	#   Function: Package
//	#
//	#   Returns the package <SymbolString> that the topic appears in.
//	#
//	#   Type-Specific Behavior:
//	#
//	#       - If the <TopicType> has scope, the package will be generated from both the title and the package passed to <New()>, not
//	#         just the package.
//	#       - If the <TopicType> is always global, the package will be the one passed to <New()>, even though it isn't part of it's
//	#         <Symbol()>.
//	#       - Everything else's package will be what was passed to <New()>, even if the title has separator symbols in it.
//	#
//	sub Package
//	    {
//	    my ($self) = @_;
//	
//	    # Headerless topics may not have a type yet.
//	    if ($self->Type() && NaturalDocs::Topics->TypeInfo($self->Type())->Scope() == ::SCOPE_START())
//	        {  return $self->Symbol();  }
//	    else
//	        {  return $self->[PACKAGE];  };
//	    };
//	
//	
//	# Function: SetPackage
//	# Replaces the package the topic appears in.  This will behave the same way as the package parameter in <New()>.  Later calls
//	# to <Package()> will still be generated according to its type-specific behavior.
//	sub SetPackage #(package)
//	    {  $_[0]->[PACKAGE] = $_[1];  };
//	
//	# Function: Using
//	# Returns an arrayref of additional scope <SymbolStrings> available to the topic via "using" statements, or undef if none.
//	sub Using
//	    {  return $_[0]->[USING];  };
//	
//	# Function: SetUsing
//	# Replaces the using arrayref of sope <SymbolStrings>.
//	sub SetUsing #(using)
//	    {  $_[0]->[USING] = $_[1];  };
//	
//	# Function: Prototype
//	# Returns the prototype if one is defined.  Will be undef otherwise.
//	sub Prototype
//	    {  return $_[0]->[PROTOTYPE];  };
//	
//	# Function: SetPrototype
//	# Replaces the function or variable prototype.
//	sub SetPrototype #(prototype)
//	    {  $_[0]->[PROTOTYPE] = $_[1];  };
//	
//	# Function: Summary
//	# Returns the topic summary, if it exists, formatted in <NDMarkup>.
//	sub Summary
//	    {  return $_[0]->[SUMMARY];  };
//	
//	# Function: Body
//	# Returns the topic's body, formatted in <NDMarkup>.  May be undef.
//	sub Body
//	    {  return $_[0]->[BODY];  };
//	
//	# Function: SetBody
//	# Replaces the topic's body, formatted in <NDMarkup>.  May be undef.
//	sub SetBody #(body)
//	    {
//	    my ($self, $body) = @_;
//	    $self->[BODY] = $body;
//	    };
//	
//	# Function: LineNumber
//	# Returns the line the topic appears at in the file.
//	sub LineNumber
//	    {  return $_[0]->[LINE_NUMBER];  };
//	
//	
//	1;

	//
	//   Substitutes certain characters with their <NDMarkup> amp chars.
	//
	private String convertAmpChars(String text) {

	    text = text.replaceAll("&","&amp;") ;
	    text = text.replaceAll("<","&lt;") ;
	    text = text.replaceAll(">","&gt;") ;
	    text = text.replaceAll("\"","&quot;") ;
	
	    return text ;
		
    }
	
	//
	//   Replaces <NDMarkup> amp chars with their original symbols.
	//
	private String restoreAmpChars(String text) {
	
	    text = text.replaceAll("&quot;","\"") ;
	    text = text.replaceAll("&gt;",">") ;
	    text = text.replaceAll("&lt;","<") ;
	    text = text.replaceAll("&amp;","&") ;

	    return text ;
		
    }

}