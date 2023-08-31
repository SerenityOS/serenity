{
  description = "Serenity OS";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, flake-utils, nixpkgs }:

    flake-utils.lib.eachDefaultSystem
      (system:
        let pkgs = nixpkgs.legacyPackages.${system}; in
        {
          formatter = pkgs.nixpkgs-fmt;
          devShells.default = import ./serenity.nix { inherit pkgs; };
        }
      );


}
