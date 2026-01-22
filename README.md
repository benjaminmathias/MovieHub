🎬 MovieHub - Android Tech Project

<hr>

Une application Android moderne démontrant l'application des dernières recommandations de Google pour le développement d'applications natives robustes et performantes.

<hr>

🛠 Stack Technique

    Langage : Kotlin (Coroutines, Flow)

    UI : Jetpack Compose (Material 3)

    Architecture : MVVM (Model-View-ViewModel) + Clean Architecture (Data/Domain/UI)

    Injection de dépendances : Hilt (Dagger)

    Réseau : Retrofit

    Base de donnée locale : Room Database (Offline-first)

    Chargement d'images : Coil   
<hr>
✨ Fonctionnalités


💾 Gestion des Favoris & Persistance

    Synchronisation Intelligente : Implémentation d'une logique de fusion (Merge) dans le Repository. Lorsqu'un film est rafraîchi via l'API, son état "Favori" stocké localement dans Room est préservé.

    Single Source of Truth (SSOT) : L'UI n'observe que la base de données locale. Cela garantit une interface stable et évite les clignotements lors des mises à jour réseau.

    Offline-first : Mise en cache locale complète via Room pour une consultation des données sans connexion réseau.

🔍 Recherche en Temps Réel

    Filtrage Dynamique : Utilisation de StateFlow pour lier la barre de recherche à la base de données, permettant un filtrage instantané des films déjà chargés.

    Gestion des États : Vues différenciées pour "Aucun résultat trouvé" et "Chargement en cours" pour une expérience utilisateur sans friction.

🎨 UI/UX Avancée

    Feedback Visuel : Intégration de Shimmer Effects sur mesure imitant la structure des cartes de films pour réduire la charge cognitive pendant le chargement.

    Navigation Intuitive : Utilisation de Jetpack Navigation pour un passage fluide entre les écrans avec gestion rigoureuse du cycle de vie des ViewModels.

📸 Aperçu

<table style="width: 100%"> <tr> <td align="center" width="50%"><b>Liste des films</b></td> <td align="center" width="50%"><b>Détails du film</b></td> </tr> <tr> <td align="center"> <img src="https://github.com/user-attachments/assets/3d45eb7c-5924-4dde-a256-817ca60aea29" height="500" /> </td> <td align="center"> <img src="https://github.com/user-attachments/assets/4faf06c0-08be-43ae-9978-918b4f80b8dd" height="500" /> </td> </tr> </table>
