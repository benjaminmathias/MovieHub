# MovieHub

MovieHub est une application Android qui affiche les films populaires et permet de rechercher et filtrer des films via l'API TMDB. Home et Search sont **Offline-First** avec pagination infinie ; Discover utilise une pagination réseau adaptée à ses filtres dynamiques.

<p align="center">
  <img src="screenshots/home_popular.png" width="200" />
  <img src="screenshots/details.png" width="200" />
  <img src="screenshots/search_result.png" width="200" />
</p>

## Fonctionnalités

- **Offline-First** : Room comme *Single Source of Truth* (SSOT) pour Home et Search. L'application fonctionne sans connexion grâce au cache local.
- **Pagination infinie** : Paging 3 avec `RemoteMediator` pour Home/Search et `PagingSource` réseau pour Discover.
- **Recherche réactive** : recherche instantanée avec debounce et gestion des états vide / erreur / hors-ligne.
- **Favoris** : sauvegarde locale, suppression par balayage avec action d'accessibilité dédiée.
- **Détail riche** : synopsis, note, durée, genres, réalisateur et distribution.
- **Thème** : clair / sombre / système, mémorisé via DataStore.
- **Mise en cache des images** : Coil (mémoire + disque), nettoyable depuis les paramètres.

## Stack technique

| Domaine | Technologie |
|---|---|
| Langage | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Navigation | Navigation 3 |
| Architecture | MVVM + couches data / domain / ui |
| Injection | Hilt |
| Réseau | Retrofit + OkHttp |
| Persistance | Room |
| Pagination | Paging 3 (`RemoteMediator` + `PagingSource` réseau) |
| Images | Coil |
| Asynchrone | Coroutines + Flow |

## Architecture

Le code est séparé en trois couches :

1. **ui** — Compose + ViewModel ; chaque écran expose un état scellé (`Loading` / `Success` / `Error`) via un `StateFlow`.
2. **domain** — modèles (`Movie`, `Actor`, `MovieCredits`) et contrats de repository.
3. **data** — Retrofit, Room, mappers et implémentations des repositories.

### Single Source of Truth (SSOT)

L'architecture dépend de l'écran :

- **Home / Search** : l'API est synchronisée par `RemoteMediator` dans Room. Room est la source de vérité, ce qui fournit un cache local et un support hors-ligne.
- **Discover** : les filtres dynamiques alimentent un `PagingSource` réseau. Les pages ne sont pas persistées dans Room ; les résultats sont seulement enrichis avec le flux local des IDs favoris afin de garder l'indicateur cohérent avec les autres écrans.

Discover reste donc network-backed par choix : persister chaque combinaison de filtres ajouterait une complexité disproportionnée pour ce projet.

### Pagination

MovieHub utilise les deux formes de Paging 3 :

- `RemoteMediator` + Room pour les flux Home et Search persistés.
- `PagingSource` réseau pour Discover, dont les résultats dépendent des filtres courants et ne sont pas mis en cache durablement.

## Tests

Trois niveaux de tests :

- **Unitaires** (`src/test`) : mappers, repository et ViewModels (coroutines, debounce, favoris, détail).
- **Instrumentés** (`src/androidTest`) : navigation, recherche/favoris, Discover et médiateurs de pagination, exécutés avec une **fausse API déterministe** (`FakeMovieApiService`) sans dépendance réseau.
- **Room/Paging** : synchronisation API ↔ base vérifiée sur une base en mémoire.

Les tests instrumentés se lancent localement avec un émulateur ou appareil connecté :

```bash
./gradlew connectedDebugAndroidTest
```

## Installation

L'application utilise The Movie Database (TMDB) comme source de données. La clé API n'est pas versionnée :

1. Clonez ce dépôt.
2. Créez un compte gratuit sur [TMDB](https://www.themoviedb.org/settings/api) pour générer une clé.
3. À la racine du projet, créez (ou ouvrez) le fichier `local.properties` et ajoutez :
   `TMDB_API_KEY=votre_cle_api_ici`
4. Synchronisez Gradle et lancez le projet.
