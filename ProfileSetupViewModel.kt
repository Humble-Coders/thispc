clc;
clear all;
format short;

% Objective function
fobj = @(x) x(1) - x(2) + 2*x(1)^2 + 2*x(1)*x(2) + x(2)^2;

% Gradient of f
gradx = @(x)[1+4*x(1)+2*x(2);-1+2*x(1)+2*x(2)];

% Hessian matrix
Hx = @(x)[4,2;2,2];

% Initial point
x0 = [1; 1];

maxiter = 4;
tol = 1e-3;
iter = 0;

X = [];    % store iterations

while norm(gradx(x0)) > tol && iter < maxiter
    
    X = [X; x0'];    % store current point
    
    % Descent direction
    S = -gradx(x0);
    
    % Hessian
    H = Hx(x0);
    
    % Step size (optimal for quadratic)
    lam = (S' * S) / (S' * H * S);
    
    % Update
    x0 = x0 + lam * S;
    
    iter = iter + 1;
end

% Final output

fprintf('Optimal solution x = [%f %f]\n', x0(1), x0(2));

fprintf('Optimal value f(x) = %f\n', fobj(x0));

% Expected Output shown in image:
% Optimal solution x = [-0.981216 1.495304]
% Optimal value f(x) = -1.249449
