# SmartBank-frontend

Interface desktop SmartBanking en JavaFX, Java 17 minimum et Maven. Le code a été déplacé depuis SmartBank-App en conservant ses packages Java et ses chemins de ressources.

## Structure

```text
src/main/java/ma/bankdirect/
  App.java                 Démarrage et fenêtre
  PrimaryController.java   Écran de connexion
  SecondaryController.java Ancienne vue de démonstration
src/main/resources/com/bankapp/
  primary.fxml             Connexion desktop
  secondary.fxml           Ancienne vue de démonstration
  auth.css                 Charte turquoise / bleu profond
```

## Lancer

```bash
mvn clean compile
mvn javafx:run
```

Dans le workspace complet, le lanceur PowerShell `../SmartBank-App/scripts/run-desktop.ps1` peut aussi être utilisé avec un runtime JavaFX local. `JAVA_HOME` doit désigner un JDK 17 ou supérieur.

## Limites actuelles

L’interface valide les identifiants auprès du backend HTTP. Le dashboard ne s’ouvre plus avec des valeurs quelconques. Les requêtes sont asynchrones, hors du thread graphique. Aucun accès SQL direct ni secret MySQL n’est nécessaire au frontend.

## Parcours d’authentification

1. Démarrer l’API dans SmartBank-backend après configuration et migration de sa base (voir son README).
2. Lancer le desktop avec `../SmartBank-App/scripts/run-desktop.cmd` ou `mvn javafx:run`.
3. Utiliser un compte présent en base. Pour `karim`, le mot de passe provisoire est choisi localement lors de l’initialisation explicite du backend.
4. Changer obligatoirement le mot de passe à la première connexion avant d’ouvrir le dashboard.
5. Utiliser Paramètres pour le changer ultérieurement, ou « Je ne peux pas me connecter » pour la récupération.

L’URL de l’API est `http://127.0.0.1:8081`, remplaçable par `SMARTBANK_API_URL` ou `-Dsmartbank.api.url`. Hors localhost, HTTPS est obligatoire. La récupération utilise le canal configuré dans le backend : e-mail SMTP (`-EmailRecovery`) ou fichier local de test (`-LocalRecovery`). L’écran adapte ses instructions au champ `delivery` de la réponse API ; aucune adresse du client ni aucun secret SMTP n’est transmis au frontend. Voir le README backend pour la configuration et la recette de l’e-mail.

`api/AuthApi` contient le client HTTP et le décodage des réponses publiques. `auth/Async` réalise les appels en arrière-plan, `auth/AuthDialogs` gère les formulaires de changement et récupération. `SessionManager` garde le jeton uniquement en mémoire. L’application observe l’activité et vérifie périodiquement la session active auprès du serveur ; inactivité ou révocation ramènent à la connexion. La fermeture ne garantit pas une révocation distante en cas de coupure : les délais backend restent applicables.

Le rôle CUSTOMER et un profil client sont requis pour ouvrir le dashboard actuel. Ses soldes et opérations restent des exemples, explicitement signalés à l’écran ; l’authentification ne raccorde pas encore ces cartes à la base.

Les maquettes, le cahier des charges personnel et les sources temporaires de vérification graphique sont conservés dans `../Livrables-locaux/`, hors des dépôts. Les dossiers `output/`, `target/` et `tmp/` sont ignorés par Git.
