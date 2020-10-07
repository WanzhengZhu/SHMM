function [RandVMF] = randVMF(N, mu, k)
% This function generates the random samples from VMF distribution. The
% formula and algorithm of the generation process is descrbed in
% SphereDistributionRand.pdf.
%
% Usage:
%   [RandVMF] = randVMF(N, mu, k);
%
% Inputs:
%   N: The number of samples one wants to generate.
%
%   mu: The mean direction of the VMF distribution. Notice that the norm of
%       mu must be 1.
%
%   k: The kappa parameter of the VMF distribution.
%
% Outputs:
%   RandVMF: A N x p matrix which contains the generated random samples
%       from VMF distribution. p the same dimension as mean direction.
%
% Function is written by Yu-Hui Chen, University of Michigan
% Contact E-mail: yuhuic@umich.edu
%

mu = mu(:)';
if(norm(mu,2)<1-0.0001 || norm(mu,2)>1+0.0001)
    error('Mu should be unit vector');
end
p = size(mu,2);
tmpMu = [1 zeros(1,p-1)];
t = randVMFMeanDir(N, k, p);
RandSphere = randUniformSphere(N, p-1);

RandVMF = repmat(t, [1 p]).*repmat(tmpMu, [N 1]) + repmat((1-t.^2).^(1/2), [1 p]).*[zeros(N,1) RandSphere];
% Rotate the distribution to the right direction
Otho = null(mu);
Rot = [mu' Otho];
RandVMF = (Rot*RandVMF')';
end