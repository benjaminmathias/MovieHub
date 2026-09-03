# Rapport d’audit Jetpack Compose — 2026-09-03

## Périmètre

Audit ciblé de `:app`, sans migration de version ni changement volontaire de comportement ou d’UI. Le flux inspecté est : Compose → ViewModel → repository → Room/Paging/Retrofit, avec navigation Navigation 3 et injection Hilt.

## Mesures et scorecard

Diagnostics générés après cleanup :

- 36/36 composables nommés restartable et skippable (100 %).
- Module : 114 composables skippables sur 161 restartable ; ce total inclut les lambdas générées.
- Strong Skipping actif avec Kotlin 2.2.10 ; aucun `@NonSkippableComposable` ni `@DontMemoize` trouvé.
- Rapports bruts : `app/build/compose_audit/app-composables.txt`, `app-composables.csv`, `app-classes.txt` et `release/app-module.json`.

| Axe | Score | Constat |
|---|---:|---|
| Performance | 8/10 | Pas de calcul ou état nouvellement identifié comme coûteux. `Movie` et `MovieCredits` restent signalés instables dans les signatures Compose ; Strong Skipping limite le risque, mais aucune annotation ou réécriture spéculative n’a été ajoutée. Voir les [bonnes pratiques de performance Compose](https://developer.android.com/develop/ui/compose/performance/bestpractices) et le [Strong Skipping mode](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping). |
| State | 9/10 | État hoisté aux écrans, `MutableStateFlow` non exposé, Paging collecté dans le composable approprié. Le remplacement de `UiText` par un ID `@StringRes` réduit une couche sans changer la source de vérité. Voir [state et hoisting](https://developer.android.com/develop/ui/compose/state) et [Paging Compose](https://developer.android.com/topic/libraries/architecture/paging/v3-compose). |
| Side effects | 9/10 | Les `LaunchedEffect`, collectors et annulations existants ont été conservés : ils portent des effets réels (swipe, réseau, lifecycle). Aucun `remember` ou collector inutile n’a été introduit. Voir [side-effects Compose](https://developer.android.com/develop/ui/compose/side-effects). |
| Composable API | 9/10 | Les extractions restantes correspondent à des responsabilités visibles ou à de la réutilisation. Deux conteneurs sans valeur et une hiérarchie de métadonnées locale inutile ont été réduits. Voir les [API guidelines Compose](https://developer.android.com/develop/ui/compose/api-guidelines). |

Score pondéré : **87/100**. La comparaison avec le rapport précédent (**85/100**) est indicative : le présent rapport couvre principalement le cleanup et les diagnostics post-refactor, pas une nouvelle direction visuelle.

## Simplifications réalisées

### SAFE

- Suppression de `core/util/UiText.kt` : cette sealed class n’avait qu’une implémentation et servait uniquement à transporter deux ressources. Les états d’erreur utilisent maintenant un `Int` annoté `@StringRes`, résolu au niveau UI.
- Suppression de `ui/navigation/BottomNavItem.kt` : la hiérarchie sealed à deux objets est devenue une petite structure privée utilisée uniquement par `NavigationRoot`.
- Suppression de `NavKey` répété sur les sous-types de `Route` : l’interface parent le fournit déjà.
- Suppression d’un `Box` autour de la `LazyColumn` de `MovieDetailContent` et d’un wrapper `Column` autour de `EmptyStateView` dans l’état erreur détail.
- Simplification de la racine de contenu de `FavoriteScreen` : le `Column` avec un unique enfant pondéré est remplacé par un `Box` plein écran, sans modifier l’état ni les callbacks.
- Aplatissement de la clé DataStore unique dans `UserPreferenceRepository`.
- Remplacement de `SharedPreferences.edit().putBoolean(...).apply()` par l’extension KTX déjà disponible.

### Code mort, ressources et Gradle

- Suppression de cinq ressources confirmées inutilisées par lint : `search`, `details`, `release_date`, `not_available`, `cast`.
- Suppression de la dépendance directe `material-icons-core` et de son alias de version ; les icônes utilisées restent fournies par `material-icons-extended`.
- Suppression d’un plugin Kotlin Serialization commenté et remplacement de l’appel DSL déprécié `pickFirst(...)` par `pickFirsts.add(...)`.

## Trois actions prioritaires appliquées

1. **[Fait]** Remplacer le wrapper d’erreur one-shot par `@StringRes Int` dans `MovieDetailUiState` et `MovieFavoriteListUiState` ; cela suit le principe de state simple des [API Compose](https://developer.android.com/develop/ui/compose/api-guidelines).
2. **[Fait]** Garder les éléments de navigation statiques privés à `NavigationRoot`, conformément aux [API guidelines](https://developer.android.com/develop/ui/compose/api-guidelines), au lieu d’exposer une hiérarchie dédiée.
3. **[Fait]** Retirer les conteneurs Compose sans responsabilité dans les écrans détail/favoris, conformément aux [bonnes pratiques de performance](https://developer.android.com/develop/ui/compose/performance/bestpractices), en conservant les mêmes modificateurs de taille utiles.

## Ce qui a été volontairement laissé tel quel

- Les ViewModels, pipelines Flow, RemoteMediators, Hilt, Room et Navigation 3 : ils portent des frontières ou des garanties d’annulation/persistance réelles ; les fusionner aurait augmenté le risque sans gain mesurable.
- Les interfaces de repository et les use cases non triviaux : ils servent de frontière testable entre données et UI.
- Les versions Gradle/AGP/Compose/AndroidX : lint les signale comme anciennes, mais la demande exclut les migrations arbitraires.
- Le thème de lancement : aucun problème de splash personnalisé n’a été introduit ; une évolution visuelle serait hors scope.
- Les warnings système de compatibilité page mémoire 16 KB observés au premier lancement (`libdatastore_shared_counter.so`, `libandroidx.graphics.path.so`) : ils proviennent de bibliothèques natives et ne sont pas causés par ce cleanup.

## Validation

- Baseline avant modification : `ktlintCheck`, `testDebugUnitTest`, `assembleDebug` et `lintDebug` réussis.
- Après modification : `:app:ktlintCheck`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` réussis.
- Lint final : 0 erreur ; warnings restants limités aux versions/SDK hors scope.
- `:app:connectedDebugAndroidTest` : **17/17**, 0 skip, 0 échec, Pixel 10a (AVD) Android 17.
- Parcours manuel ADB/UI tree : accueil → favoris → retour, paramètres → retour, accueil → détail → retour ; titres, actions d’accessibilité et bornes UI présents.
- Buffer `logcat -b crash` : aucun crash `com.benjamin.moviehub` observé.

## Métriques du diff

- 2 fichiers supprimés.
- 48 lignes nettes supprimées sur le diff de cleanup, hors changements préexistants `.idea` et artefacts locaux.
- 5 ressources supprimées.
- 1 dépendance directe supprimée.
- 0 fonctionnalité, écran ou test supprimé.

## Suite possible, hors scope

Une optimisation ultérieure de la stabilité des modèles `Movie`/`MovieCredits` pourrait être étudiée si un profilage montre des recompositions coûteuses. Elle nécessiterait une mesure avant/après et une vérification de compatibilité avec Room, Paging et les mappings ; elle n’est donc pas justifiée par cet audit seul.
