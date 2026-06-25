# October CMS Support

PhpStorm plugin with October CMS navigation and completion helpers.

## Features

### Twig Tags

- Autocomplete for common October CMS Twig tags inside `{% ... %}`.
- Completing `partial` inserts the partial template with quotes:

```twig
{% par %}
```

becomes:

```twig
{% partial "" %}
```

The caret is placed between the quotes.

### Partials

- Ctrl+Click / Go to Declaration for regular partials:

```twig
{% partial "journal/list-category" category=category list=categories %}
{{ partial('journal/list-category', { category: category }) }}
```

Resolves to `themes/<theme>/partials/journal/list-category.htm`.

- Autocomplete for partial names inside `{% partial "..." %}` and `partial('...')`.
- Autocomplete uses the current theme `partials/` directory as the root.

### AJAX Partials

- Ctrl+Click / Go to Declaration for AJAX partials:

```twig
{% ajaxPartial 'counter' lazy %}
{{ ajaxPartial('counter') }}
```

Resolves to `themes/<theme>/partials/counter.htm`.

- Autocomplete for partial names inside `{% ajaxPartial "..." %}`, `{% ajaxPartial '...' %}`, and `ajaxPartial('...')`.

### Components

- Ctrl+Click / Go to Declaration for page component blocks:

```ini
[PressCenterPosts]
```

Resolves registered component aliases from plugin `Plugin.php` files, including entries like:

```php
PressCenterPostsComponent::class => 'PressCenterPosts'
```

The plugin opens the matching PHP component class.

- Autocomplete for registered and conventional component aliases inside component blocks:

```ini
[Press]
```

can suggest aliases such as `PressCenterPosts`.

- Component completion shows the owning plugin on the right, for example `Webinsane.Bcc` or `acme.blog`.

- Unknown component aliases are highlighted in page configuration blocks.

- Autocomplete for component property names inside component configuration blocks:

```ini
[PressCenterPosts]
sl
```

The suggestions are read from the component class `defineProperties()` method.
Selecting a property inserts the assignment with its `default` value when one is declared:

```ini
slug = "{{ :slug }}"
```

The caret is placed inside the value so it can be edited immediately.

- Unknown component properties are highlighted when the component declares available properties.

### Pages

- Ctrl+Click / Go to Declaration for the October CMS `|page` Twig filter:

```twig
{{ 'about/press-center-new/post'|page({ slug: topPost.slug }) }}
```

Resolves to `themes/<theme>/pages/about/press-center-new/post.htm`.

- Ctrl+Click / Go to Declaration for `pageUrl(...)`:

```twig
{{ pageUrl('about/press-center-new/post', { slug: topPost.slug }) }}
```

- Autocomplete for page names inside `'...'|page` and `pageUrl('...')`.

### Layouts

- Ctrl+Click / Go to Declaration for page layout settings:

```ini
layout = "main"
```

Resolves to `themes/<theme>/layouts/main.htm`.

- Autocomplete for layout names inside `layout = "..."`.

### Content

- Ctrl+Click / Go to Declaration for content tags and content function calls:

```twig
{% content "blocks/intro.htm" %}
{{ content('blocks/intro') }}
```

Resolves to files under `themes/<theme>/content/`.

- Autocomplete for content file names inside `{% content "..." %}` and `content('...')`.

### Diagnostics And Quick Fixes

- Missing theme files are highlighted for:
  - `{% partial "..." %}`
  - `{% ajaxPartial "..." %}`
  - `partial('...')`
  - `ajaxPartial('...')`
  - `[ComponentAlias]`
  - `layout = "..."`
  - `{% content "..." %}`
  - `content('...')`
  - `'...'|page`
  - `pageUrl('...')`

- Alt+Enter can create the missing page, layout, partial, or content file in the current theme.

### PHP Code Sections

- PHP syntax highlighting for October CMS template code sections:

```php
==
function onStart()
{
    $this['category'] = $this->param('category');
}
==
```

The plugin highlights the code between the first two `==` section separators as PHP in theme `pages/`, `layouts/`, and `partials/` files.

- Reformat Code normalizes indentation inside the PHP section.
- The action `October CMS: Format PHP Section` formats the PHP section directly in the current template. It is available from Find Action and the Code menu.
- Pressing Enter inside PHP blocks keeps the next line indented.
- Typing `}` inside a PHP section reindents the closing brace line.
- Autocomplete inside PHP sections includes October lifecycle methods, `$this` helpers, and common PHP keywords.
- Autocomplete also suggests local PHP variables declared earlier in the section; both `q` and `$q` can match `$qq`.
- Completion popup opens automatically while typing inside PHP sections and selects the first item by default; `Ctrl+Space` still works.

```php
function onStart()
{
    $this['category'] = $this->param('category');
}
```

## Build

```powershell
.\gradlew.bat buildPlugin
```

The built plugin ZIP is written to `build/distributions/`.
