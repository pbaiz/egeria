/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.subjectarea.fvt;

import org.odpi.openmetadata.accessservices.subjectarea.SubjectArea;
import org.odpi.openmetadata.accessservices.subjectarea.SubjectAreaCategory;
import org.odpi.openmetadata.accessservices.subjectarea.client.SubjectAreaImpl;
import org.odpi.openmetadata.accessservices.subjectarea.ffdc.exceptions.InvalidParameterException;
import org.odpi.openmetadata.accessservices.subjectarea.ffdc.exceptions.SubjectAreaCheckedExceptionBase;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.category.Category;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.glossary.Glossary;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.nodesummary.CategorySummary;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.nodesummary.GlossarySummary;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * FVT to call subject area category client API to create a category hierarchy,
 * based on given DEPTH and WIDTH values.
 */
public class CategoryHierarchyFVT
{
    private static final String DEFAULT_TEST_GLOSSARY_NAME = "Test Glossary for category hierarchy FVT";
    private static final String DEFAULT_TEST_CATEGORY_NAME_BASE = "Test hierarchy category ";

    private static final int WIDTH = 3;
    private static final int DEPTH = 4;
    private static int depth_counter = 0;

    private static SubjectAreaCategory subjectAreaCategory = null;
    private GlossaryFVT glossaryFVT =null;
    private String url = null;
    private String glossaryGuid = null;

    public static void main(String args[])
    {
        SubjectArea subjectArea = null;
        String url = null;
        try
        {
            url = RunAllFVT.getUrl(args);
            CategoryHierarchyFVT categoryHierarchyFVT =new CategoryHierarchyFVT(url);
            categoryHierarchyFVT.run();
        } catch (IOException e1)
        {
            System.out.println("Error getting user input");
        } catch (SubjectAreaCheckedExceptionBase e)
        {
            System.out.println("ERROR: " + e.getErrorMessage() + " Suggested action: " + e.getReportedUserAction());
        }

    }
    public static void runit(String url) throws SubjectAreaCheckedExceptionBase
    {
        CategoryHierarchyFVT fvt =new CategoryHierarchyFVT(url);
        fvt.run();
    }

    public CategoryHierarchyFVT(String url) throws InvalidParameterException
    {
        subjectAreaCategory = new SubjectAreaImpl(FVTConstants.SERVER_NAME1,url).getSubjectAreaCategory();
        glossaryFVT = new GlossaryFVT(url,FVTConstants.SERVER_NAME1);
        this.url=url;
    }

    public void run() throws SubjectAreaCheckedExceptionBase
    {
        SubjectArea subjectArea = null;

        System.out.println("Create a glossary");
        Glossary glossary = glossaryFVT.createGlossary(DEFAULT_TEST_GLOSSARY_NAME);
        FVTUtils.validateNode(glossary);
        String glossaryGuid = glossary.getSystemAttributes().getGUID();
        System.out.println("Create category hierarchy");
        Set<Category> categories = createTopCategories(glossaryGuid);
        while (depth_counter < DEPTH)
        {
            depth_counter++;
            Set<Category> childrenCategories = new HashSet();
            for (Category category : categories)
            {
                FVTUtils.validateNode(category);
                childrenCategories = createChildrenCategories(category,glossaryGuid);
            }
            categories = childrenCategories;
        }
    }

    /**
     * Create top categories i.e. categories with no parent category
     * @param glossaryGuid glossary guid
     * @return a set of created categories
     * @throws SubjectAreaCheckedExceptionBase an error occurred.
     */
    private static Set<Category> createTopCategories(String glossaryGuid) throws SubjectAreaCheckedExceptionBase
    {
        Set<Category> categories = new HashSet();
        for (int width_counter = 0; width_counter < WIDTH; width_counter++)
        {
            String categoryName = createName(0, width_counter);
            Category category = CategoryHierarchyFVT.createCategoryWithGlossaryGuid(categoryName,glossaryGuid);
            FVTUtils.validateNode(category);
            System.out.println("Created category with name  " + categoryName + " with no parent");
            categories.add(category);
        }
        return categories;
    }

    /**
     * Derive a category name based on a base string a DEPTH and a WIDTH
     *
     * @param depth DEPTH of hierarchy
     * @param width WIDTH of hierarchy
     * @return category name
     */
    private static String createName(int depth, int width)
    {
        return DEFAULT_TEST_CATEGORY_NAME_BASE + "d" + depth + "w" + width;
    }

    /**
     * Create children categories i.e. categories under the supplied parent category
     *
     * @param parent parent category
     * @param glossaryGuid guid of the associated glossary
     * @return a set of created categories
     * @throws SubjectAreaCheckedExceptionBase an error occurred.
     */
    private static Set<Category> createChildrenCategories(Category parent,String glossaryGuid) throws SubjectAreaCheckedExceptionBase
    {

        Set<Category> categories = new HashSet<>();
        for (int width_counter = 0; width_counter < WIDTH; width_counter++)
        {
            String categoryName = createName(depth_counter, width_counter);
            Category category = createCategoryWithParentGlossary(categoryName, parent, glossaryGuid);
            FVTUtils.validateNode(category);
            System.out.println("Created category with name  " + categoryName + " with parent " + parent.getName());
            categories.add(category);
        }
        return categories;
    }

    /**
     * Create a category associated under a parent category and associate with the named glossary
     *
     * @param categoryName name of the category to create
     * @param parent       category under which to create this category
     * @param glossaryGuid guid of the associated glossary
     * @return created category
     * @throws SubjectAreaCheckedExceptionBase
     */
    private static Category createCategoryWithParentGlossary(String categoryName, Category parent, String glossaryGuid) throws SubjectAreaCheckedExceptionBase
    {
        Category category = new Category();
        category.setName(categoryName);
        GlossarySummary glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        category.setGlossary(glossarySummary);
        CategorySummary parentCategorysummary = new CategorySummary();
        parentCategorysummary.setGuid(parent.getSystemAttributes().getGUID());
        category.setParentCategory(parentCategorysummary);
        Category newCategory = subjectAreaCategory.createCategory(FVTConstants.SERVER_NAME1,FVTConstants.USERID, category);
        FVTUtils.validateNode(newCategory);
        System.out.println("Created Category " + newCategory.getName() + " with guid " + newCategory.getSystemAttributes().getGUID());
        return newCategory;
    }

    /**
     * Create a category associated with a glossary, identified with a guid.
     *
     * @param categoryName name of the category to create
     * @param glossaryGuid guid of the glossary to associate with this category
     * @return created category
     * @throws SubjectAreaCheckedExceptionBase error
     */
    public static Category createCategoryWithGlossaryGuid(String categoryName, String glossaryGuid) throws SubjectAreaCheckedExceptionBase
    {
        Category category = new Category();
        category.setName(categoryName);
        GlossarySummary glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        category.setGlossary(glossarySummary);
        Category newCategory = subjectAreaCategory.createCategory(FVTConstants.SERVER_NAME1,FVTConstants.USERID, category);
        FVTUtils.validateNode(newCategory);
        System.out.println("Created Category " + newCategory.getName() + " with guid " + newCategory.getSystemAttributes().getGUID());
        return newCategory;
    }
}
