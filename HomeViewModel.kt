clc;
clear;

M = 1e6;

% Cost coefficients
C = [2 3 0 M M 0];

% Initial Tableau
A = [
    1 1 -1 1 0 0 4;
    1 2  0 0 1 0 6;
    2 1  0 0 0 1 8
];

% Initial basic variables: a1,a2,s3
BV = [4 5 6];

[m,n] = size(A);
n = n-1;   % last column is RHS

while true

    % Compute Zj
    Zj = zeros(1,n+1);

    for i = 1:m
        Zj = Zj + C(BV(i))*A(i,:);
    end

    % Compute Cj-Zj
    CJ_ZJ = [C 0] - Zj;

    fprintf('\nCurrent Tableau:\n');
    disp(A);

    fprintf('Cj-Zj:\n');
    disp(CJ_ZJ);

    % Optimality test for minimization
    [minval, pivot_col] = min(CJ_ZJ(1:n));

    if minval >= 0
        break;
    end

    % Ratio Test
    ratio = inf(m,1);

    for i = 1:m
        if A(i,pivot_col) > 0
            ratio(i) = A(i,end)/A(i,pivot_col);
        end
    end

    [~, pivot_row] = min(ratio);

    % Update basic variable
    BV(pivot_row) = pivot_col;

    % Pivot Operation
    pivot = A(pivot_row,pivot_col);
    A(pivot_row,:) = A(pivot_row,:) / pivot;

    for i = 1:m
        if i ~= pivot_row
            A(i,:) = A(i,:) - A(i,pivot_col)*A(pivot_row,:);
        end
    end

end

% Final solution
x = zeros(n,1);

for i = 1:m
    x(BV(i)) = A(i,end);
end

fprintf('\nOptimal Solution:\n');
disp(x);

Z = C*x;

fprintf('Minimum Z = %f\n',Z);
