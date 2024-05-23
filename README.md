# Workflow-release-template

This template contains boilerplate code for the gradle release. The Gradle release workflow under GitHub Actions would 
quickly create a new release.

Anyone with access to the template repository can create a new repository based on the template 
with the same directory structure, branches, and files.

# How to use template repository

1. Create a new repository.
2. Choose the Repository Template from the dropdown.

After you make your repository a template, anyone with access to the repository can generate a new repository with the same directory structure and files as your default branch.

# Plugin development

Before starting with the plugin development, Make sure you have right settings in place.

1. Please check and update _`Dependencies`_, _`Version`_, _`Description`_  iN `build.gradle` file.
2. _`rootProject.name`_ in `settings.gradle` file.

After the plugin development , please follow the below steps to make release.

1. Navigate to the Actions tab. 
2. On the left hand side , choose `_Gradle release workflow_` under _`All workflows`_ . 
3. You would see _`Run workflow`_ button on the right side to run the release.