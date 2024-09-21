{
  description = "Serenity";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      utils,
    }:
    utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = import ./Toolchain { inherit pkgs; };
        devShells.ladybird = import ./Ladybird { inherit pkgs; };
        formatter = pkgs.nixfmt-rfc-style;
      }
    );
}
