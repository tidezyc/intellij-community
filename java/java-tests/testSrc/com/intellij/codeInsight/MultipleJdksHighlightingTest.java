/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.codeInsight;

import com.intellij.idea.Bombed;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import com.intellij.util.Consumer;

import java.util.Calendar;

public class MultipleJdksHighlightingTest extends UsefulTestCase {

  private CodeInsightTestFixture myFixture;
  private Module myJava6Module;
  private Module myJava7Module;
  private Module myJava8Module;

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public MultipleJdksHighlightingTest() {
    IdeaTestCase.initPlatformPrefix();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    myFixture.tearDown();
    myFixture = null;
    myJava6Module = null;
    myJava7Module = null;
    myJava8Module = null;
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
    myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
    myFixture.setTestDataPath(PathManagerEx.getTestDataPath() + "/codeInsight/multipleJdks");
    final JavaModuleFixtureBuilder[] builders = new JavaModuleFixtureBuilder[3];
    builders[0] = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
    builders[1] = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
    builders[2] = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
    myFixture.setUp();
    myJava6Module = builders[0].getFixture().getModule();
    myJava7Module = builders[1].getFixture().getModule();
    myJava8Module = builders[2].getFixture().getModule();
    ModuleRootModificationUtil.updateModel(myJava6Module, new Consumer<ModifiableRootModel>() {
      @Override
      public void consume(ModifiableRootModel model) {
        model.addModuleOrderEntry(myJava7Module);
        model.setSdk(IdeaTestUtil.getMockJdk17());
        String contentUrl = VfsUtilCore.pathToUrl(myFixture.getTempDirPath()) + "/java6";
        model.addContentEntry(contentUrl).addSourceFolder(contentUrl, false);
      }
    });

    ModuleRootModificationUtil.updateModel(myJava7Module, new Consumer<ModifiableRootModel>() {
      @Override
      public void consume(ModifiableRootModel model) {
        model.addModuleOrderEntry(myJava8Module);
        model.setSdk(IdeaTestUtil.getMockJdk17());
        String contentUrl = VfsUtilCore.pathToUrl(myFixture.getTempDirPath()) + "/java7";
        model.addContentEntry(contentUrl).addSourceFolder(contentUrl, false);
      }
    });

    ModuleRootModificationUtil.updateModel(myJava8Module, new Consumer<ModifiableRootModel>() {
      @Override
      public void consume(ModifiableRootModel model) {
        model.setSdk(IdeaTestUtil.getMockJdk18());
        String contentUrl = VfsUtilCore.pathToUrl(myFixture.getTempDirPath()) + "/java8";
        model.addContentEntry(contentUrl).addSourceFolder(contentUrl, false);
      }
    });
  }

  public void testGetClass() throws Exception {
    doTest();
  }

  public void testWrongSuperInLibrary() throws Exception {
    final String name = getTestName(false);
    for (Module module : new Module[] {myJava7Module, myJava8Module}) {
      ModuleRootModificationUtil.updateModel(module, new Consumer<ModifiableRootModel>() {
        @Override
        public void consume(ModifiableRootModel model) {
          ClsGenericsHighlightingTest.commitLibraryModel(model, myFixture.getTestDataPath(), name + ".jar");
        }
      });
    }

    myFixture.configureByFile("java8/p/" + name + ".java");
    myFixture.checkHighlighting();
  }

  public void testWrongComparator() throws Exception {
   doTestWithoutLibrary();
  }

  public void testGenericComparator() throws Exception {
    doTestWithoutLibrary();
  }

  public void testGenericCallableWithDifferentTypeArgs() throws Exception {
    doTestWithoutLibrary();
  }

  public void testSuperclassImplementsUnknownType() throws Exception {
    doTestWithoutLibrary();
  }

  public void testDeclaredTypeOfVariableImplementsUnknownType() throws Exception {
    doTestWithoutLibrary();
  }
  
  public void testSuperclassImplementsGenericsOfUnknownType() throws Exception {
    doTestWithoutLibrary();
  }

  public void testSuperMethodNotExist() throws Exception {
    doTestWithoutLibrary();
  }

  public void testNoOverriding() throws Exception {
    doTestWithoutLibrary();
  }

  public void testStaticCallOnChildWithNotAccessibleParent() throws Exception {
    doTest3Modules();
  }

  private void doTestWithoutLibrary() {
    final String name = getTestName(false);
    myFixture.configureByFiles("java7/p/" + name + ".java", "java8/p/" + name + ".java");
    myFixture.checkHighlighting();
  }

  private void doTest3Modules() {
    final String name = getTestName(false);
    myFixture.configureByFiles("java6/p/" + name + ".java", "java7/p/" + name + ".java", "java8/p/" + name + ".java");
    myFixture.checkHighlighting();
  }


  private void doTest() {
    final String name = getTestName(false);
    for (Module module : new Module[] {myJava7Module, myJava8Module}) {
      ModuleRootModificationUtil.updateModel(module, new Consumer<ModifiableRootModel>() {
        @Override
        public void consume(ModifiableRootModel model) {
          ClsGenericsHighlightingTest.commitLibraryModel(model, myFixture.getTestDataPath(), name + ".jar");
        }
      });
    }

    myFixture.configureByFiles("java7/p/" + name + ".java", "java8/p/" + name + ".java");
    myFixture.checkHighlighting();
  }
}
